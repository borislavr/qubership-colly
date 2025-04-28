package org.qubership.colly;

import io.kubernetes.client.openapi.models.V1Namespace;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.inject.Singleton;

@Singleton
@IfBuildProperty(name = "environment.resolver", stringValue = "byLabel")
public class EnvironmentResolverStrategyByLabel implements EnvironmentResolverStrategy {

    public static final String ENVIRONMENT_NAME = "environmentName";

    @Override
    public String resolveEnvironmentName(V1Namespace namespace) {

        if (namespace.getMetadata() == null) {
            throw new IllegalArgumentException("Metadata for namespace '" + namespace.getMetadata() + "' is not found");
        }
        if (namespace.getMetadata().getLabels() == null) {
            return namespace.getMetadata().getName();
        }
        return namespace.getMetadata().getLabels().getOrDefault(ENVIRONMENT_NAME, namespace.getMetadata().getName());
    }
}
