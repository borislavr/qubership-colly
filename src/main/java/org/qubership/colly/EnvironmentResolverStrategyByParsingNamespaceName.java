package org.qubership.colly;

import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Optional;

@Singleton
@IfBuildProperty(name = "environment.resolver.strategy", stringValue = "byName")
public class EnvironmentResolverStrategyByParsingNamespaceName implements EnvironmentResolverStrategy {

    private static final String[] ACCEPTABLE_NAMESPACE_SUFFIXES = new String[]{"-bss", "-oss", "-data-management", "-core"};

    @Override
    public String resolveEnvironmentName(V1Namespace namespace) {
        V1ObjectMeta metadata = namespace.getMetadata();
        if (metadata == null) {
            throw new IllegalStateException("Metadata for namespace '" + namespace.getMetadata() + "' is not found");
        }
        String name = metadata.getName();
        Optional<String> suffixOpt = Arrays.stream(ACCEPTABLE_NAMESPACE_SUFFIXES)
                .filter(suffix -> name.endsWith(suffix))
                .findFirst();
        return suffixOpt.map(suffix -> name.substring(0, name.length() - suffix.length())).orElse(name);
    }
}
