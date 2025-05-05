package org.qubership.colly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.compress.utils.Lists;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.colly.cloudpassport.CloudPassport;
import org.qubership.colly.cloudpassport.CloudPassportEnvironment;
import org.qubership.colly.cloudpassport.envgen.CloudData;
import org.qubership.colly.cloudpassport.envgen.CloudPassportData;
import org.qubership.colly.cloudpassport.envgen.EnvDefinition;
import org.qubership.colly.cloudpassport.envgen.Inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class CloudPassportLoader {

    private static final String CLOUD_PASSPORT_FOLDER = "cloud-passport";

    @Inject
    GitService gitService;

    @ConfigProperty(name = "cloud.passport.folder")
    String cloudPassportFolder;

    @ConfigProperty(name = "env.instances.repo")
    Optional<String> gitRepoUrl;

    private void cloneGitRepository() {
        if (gitRepoUrl.isEmpty()) {
            Log.error("gitRepoUrl parameter is not set. Skipping repository cloning.");
            return;
        }
        File directory = new File(cloudPassportFolder);
        if (directory.exists()) {
            Log.info("Repository was already cloned. Directory: " + directory);
            return;
        }

        String gitRepoUrlValue = gitRepoUrl.get();
        gitService.cloneRepository(gitRepoUrlValue, directory);
    }

    public List<CloudPassport> loadCloudPassports() {
        cloneGitRepository();
        Path dir = Paths.get(cloudPassportFolder);
        if (!dir.toFile().exists()) {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.resolve(CLOUD_PASSPORT_FOLDER))
                    .filter(Files::isDirectory)
                    .map(path -> processYamlFilesInClusterFolder(path, path.getParent()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            Log.error("Error loading CloudPassports from " + dir, e);
        }
        return Collections.emptyList();
    }


    private CloudPassport processYamlFilesInClusterFolder(Path cloudPassportFolderPath, Path clusterFolderPath) {
        Log.info("Loading Cloud Passport from " + cloudPassportFolderPath);
        String clusterName = clusterFolderPath.getFileName().toString();
        List<CloudPassportEnvironment> environments = processEnvironmentsInClusterFolder(clusterFolderPath);
        CloudPassportData cloudPassportData;
        try (Stream<Path> paths = Files.list(cloudPassportFolderPath)) {
            cloudPassportData = paths
                    .filter(path -> path.getFileName().toString().equals(clusterName + ".yml"))
                    .map(this::parseCloudPassportDataFile)
                    .findFirst().orElseThrow();
        } catch (Exception e) {
            Log.error("Error loading Cloud Passport from " + cloudPassportFolderPath, e);
            return null;
        }

        String token;
        try (Stream<Path> credsPath = Files.list(cloudPassportFolderPath)) {
            token = credsPath
                    .filter(path -> path.getFileName().toString().equals(clusterName + "-creds.yml"))
                    .map(path -> parseTokenFromCredsFile(path, cloudPassportData))
                    .findFirst().orElseThrow();

        } catch (Exception e) {
            Log.error("Error loading Cloud Passport from " + cloudPassportFolderPath, e);
            return null;
        }
        CloudData cloud = cloudPassportData.getCloud();
        String cloudApiHost = cloud.getCloudProtocol() + "://" + cloud.getCloudApiHost() + ":" + cloud.getCloudApiPort();
        return new CloudPassport(clusterName, token, cloudApiHost, environments);
    }

    private List<CloudPassportEnvironment> processEnvironmentsInClusterFolder(Path clusterFolderPath) {

        try (Stream<Path> paths = Files.walk(clusterFolderPath)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.resolve("env_definition.yml"))
                    .filter(Files::isRegularFile)
                    .map(this::processEnvDefinition)
                    .toList();
        } catch (Exception e) {
            Log.error("Error loading Environments from " + clusterFolderPath, e);
        }
        return Lists.newArrayList();
    }

    private CloudPassportEnvironment processEnvDefinition(Path path) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            EnvDefinition envDefinition = mapper.readValue(inputStream, EnvDefinition.class);
            Inventory inventory = envDefinition.getInventory();
            Log.info("Processing environment " + inventory.getEnvironmentName());
            return new CloudPassportEnvironment(inventory.getEnvironmentName(), inventory.getDescription(), Lists.newArrayList());
        } catch (IOException e) {
            throw new RuntimeException("Error during read file: " + path, e);
        }
    }

    String parseTokenFromCredsFile(Path path, CloudPassportData cloudPassportData) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            JsonNode jsonNode = mapper.readTree(inputStream);
            JsonNode tokenNode = jsonNode.get(cloudPassportData.getCloud().getCloudDeployToken());
            if (tokenNode != null) {
                return tokenNode.findValue("secret").asText();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error during read file: " + path, e);
        }
        throw new RuntimeException("Can't read cloud passport data creds from " + path);
    }

    CloudPassportData parseCloudPassportDataFile(Path filePath) {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (FileInputStream inputStream = new FileInputStream(filePath.toFile())) {
            CloudPassportData data = mapper.readValue(inputStream, CloudPassportData.class);
            if (data != null && data.getCloud() != null) {
                return data;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error during read file: " + filePath, e);
        }
        throw new RuntimeException("Can't read cloud passport data from " + filePath);
    }
}
