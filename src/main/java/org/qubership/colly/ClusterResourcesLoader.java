package org.qubership.colly;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.colly.cloudpassport.CloudPassport;
import org.qubership.colly.cloudpassport.CloudPassportEnvironment;
import org.qubership.colly.cloudpassport.CloudPassportNamespace;
import org.qubership.colly.db.data.Cluster;
import org.qubership.colly.db.data.Environment;
import org.qubership.colly.db.data.EnvironmentType;
import org.qubership.colly.db.data.Namespace;
import org.qubership.colly.monitoring.MonitoringService;
import org.qubership.colly.db.ClusterRepository;
import org.qubership.colly.db.EnvironmentRepository;
import org.qubership.colly.db.NamespaceRepository;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@ApplicationScoped
public class ClusterResourcesLoader {

    static final String LABEL_DISCOVERY_CLI_IO_LEVEL = "discovery.cli.io/level";
    static final String LABEL_DISCOVERY_CLI_IO_TYPE = "discovery.cli.io/type";
    static final String LABEL_LEVEL_INFRA = "infra";
    static final String LABEL_LEVEL_APPS = "apps";
    static final String LABEL_TYPE_CORE = "core";
    static final String LABEL_TYPE_CSE_TOOLSET = "cse-toolset";

    @Inject
    NamespaceRepository namespaceRepository;
    @Inject
    ClusterRepository clusterRepository;
    @Inject
    EnvironmentRepository environmentRepository;
    @Inject
    MonitoringService monitoringService;

    @ConfigProperty(name = "colly.config-map.versions.name")
    String versionsConfigMapName;

    @ConfigProperty(name = "colly.config-map.versions.data-field-name")
    String versionsConfigMapDataFieldName;


    @Transactional
    public void loadClusterResources(CloudPassport cloudPassport) {
        AccessTokenAuthentication authentication = new AccessTokenAuthentication(cloudPassport.token());
        try {
            ApiClient client = ClientBuilder.standard()
                    .setAuthentication(authentication)
                    .setBasePath(cloudPassport.cloudApiHost())
                    .setVerifyingSsl(false)
                    .build();
            CoreV1Api coreV1Api = new CoreV1Api(client);
            loadClusterResources(coreV1Api, cloudPassport);
        } catch (RuntimeException | IOException e) {
            Log.error("Can't load resources from cluster " + cloudPassport.name(), e);
        }
    }

    //for testing purposes
    void loadClusterResources(CoreV1Api coreV1Api, CloudPassport cloudPassport) {
        Cluster cluster = clusterRepository.findByName(cloudPassport.name());
        if (cluster == null) {
            cluster = new Cluster(cloudPassport.name());
            Log.info("Cluster " + cloudPassport.name() + " not found in db. Creating new one.");
            clusterRepository.persist(cluster);
        }

        //it is required to set links to cluster only if it was saved to db. so need to invoke persist two
        cluster.environments = loadEnvironments(coreV1Api, cluster, cloudPassport.environments(), cloudPassport.monitoringUrl());
        clusterRepository.persist(cluster);
        Log.info("Cluster " + cloudPassport.name() + " loaded successfully.");
    }

    private List<Environment> loadEnvironments(CoreV1Api coreV1Api, Cluster cluster, List<CloudPassportEnvironment> environments, URI monitoringUri) {

        CoreV1Api.APIlistNamespaceRequest apilistNamespaceRequest = coreV1Api.listNamespace();
        Map<String, V1Namespace> k8sNamespaces;
        try {
            V1NamespaceList list = apilistNamespaceRequest.execute();
            k8sNamespaces = list.getItems().stream().collect(Collectors.toMap(v1Namespace -> getNameSafely(v1Namespace.getMetadata()), Function.identity()));

        } catch (ApiException e) {
            k8sNamespaces = new HashMap<>();
            Log.error("Can't load namespaces from cluster " + cluster.name + ". " + e.getMessage());
        }

        List<Environment> envs = new ArrayList<>();
        Log.info("Namespaces are loaded for " + cluster.name + ". Count is " + k8sNamespaces.size() + ". Environments count = " + environments.size());
        for (CloudPassportEnvironment cloudPassportEnvironment : environments) {
            Environment environment = environmentRepository.findByNameAndCluster(cloudPassportEnvironment.name(), cluster.name);
            Log.info("Start working with env = " + cloudPassportEnvironment.name());
            EnvironmentType environmentType;
            if (environment == null) {
                environment = new Environment(cloudPassportEnvironment.name());
                environment.description = cloudPassportEnvironment.description();
                environment.cluster = cluster;
                environmentType = EnvironmentType.UNDEFINED;
                environmentRepository.persist(environment);
                Log.info("env created in db: " + environment.name);
            } else {
                environmentType = environment.type;
                Log.info("environment " + environment.name + " exists");
            }
            StringBuilder deploymentVersions = new StringBuilder();

            for (CloudPassportNamespace cloudPassportNamespace : cloudPassportEnvironment.namespaceDtos()) {
                V1Namespace v1Namespace = k8sNamespaces.get(cloudPassportNamespace.name());
                if (v1Namespace == null) {
                    Log.warn("Namespace with name=" + cloudPassportNamespace.name() + " not found in cluster " + cluster.name + ". Skipping it.");
                    continue;
                }
                String namespaceUid = v1Namespace.getMetadata().getUid();
                Namespace namespace = namespaceRepository.findByUid(namespaceUid);
                if (namespace == null) {
                    namespace = new Namespace();
                    namespace.uid = namespaceUid;
                    namespace.cluster = cluster;
                    namespace.environment = environment;
                    environment.addNamespace(namespace);
                    environmentType = calculateEnvironmentType(v1Namespace, environmentType);
                }
                namespace.name = cloudPassportNamespace.name();
                deploymentVersions.append(loadInformationAboutDeploymentVersion(coreV1Api, cloudPassportNamespace.name()));
                namespaceRepository.persist(namespace);
            }
            environment.monitoringData = monitoringService.loadMonitoringData(monitoringUri, environment.getNamespaces().stream().map(namespace -> namespace.name).toList());
            environment.type = environmentType;
            environment.deploymentVersion = deploymentVersions.toString();
            environmentRepository.persist(environment);

            envs.add(environment);

        }
        return envs;
    }

    private String loadInformationAboutDeploymentVersion(CoreV1Api coreV1Api, String namespaceName) {
        CoreV1Api.APIlistNamespacedConfigMapRequest request = coreV1Api.listNamespacedConfigMap(namespaceName).fieldSelector("metadata.name=" + versionsConfigMapName);
        V1ConfigMapList configMapList;
        try {
            configMapList = request.execute();
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        if (configMapList.getItems().isEmpty()) {
            Log.warn("No config map with name=" + versionsConfigMapName + " found in namespace " + namespaceName);
            return "";
        }
        V1ConfigMap configMap = configMapList.getItems().getFirst();
        return configMap.getData().get(versionsConfigMapDataFieldName) + "\n";
    }


    private String getNameSafely(V1ObjectMeta meta) {
        if (meta == null) {
            return "<empty_name>";
        }
        return meta.getName();
    }

    private EnvironmentType calculateEnvironmentType(V1Namespace v1Namespace, EnvironmentType defaultEnvType) {
        Map<String, String> labels = Objects.requireNonNull(v1Namespace.getMetadata()).getLabels();
        String levelValue = labels.get(LABEL_DISCOVERY_CLI_IO_LEVEL);
        if (LABEL_LEVEL_APPS.equals(levelValue)) {
            String typeValue = labels.get(LABEL_DISCOVERY_CLI_IO_TYPE);
            if (LABEL_TYPE_CORE.equals(typeValue)) {
                return EnvironmentType.ENVIRONMENT;
            }
            if (LABEL_TYPE_CSE_TOOLSET.equals(typeValue)) {
                return EnvironmentType.CSE_TOOLSET;
            }
            return EnvironmentType.ENVIRONMENT;
        }
        if (LABEL_LEVEL_INFRA.equals(levelValue)) {
            return EnvironmentType.INFRASTRUCTURE;
        }
        return defaultEnvType;
    }
}
