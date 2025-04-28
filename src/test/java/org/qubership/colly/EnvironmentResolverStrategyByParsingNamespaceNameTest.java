package org.qubership.colly;


import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class EnvironmentResolverStrategyByParsingNamespaceNameTest {

    private @NotNull V1Namespace prepareNamespace(String name) {
        V1Namespace namespace = new V1Namespace();
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(name);
        namespace.setMetadata(metadata);
        return namespace;
    }

    @Test
    public void resolveNamespace() {
        EnvironmentResolverStrategyByParsingNamespaceName strategy = new EnvironmentResolverStrategyByParsingNamespaceName();
        V1Namespace namespace = prepareNamespace("my-namespace");

        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("my-namespace"));
    }

    @Test
    public void namespace_with_core_suffix() {
        EnvironmentResolverStrategyByParsingNamespaceName strategy = new EnvironmentResolverStrategyByParsingNamespaceName();
        V1Namespace namespace = prepareNamespace("env-core");

        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("env"));
    }

    @Test
    public void namespace_with_only_core_suffix() {
        EnvironmentResolverStrategyByParsingNamespaceName strategy = new EnvironmentResolverStrategyByParsingNamespaceName();
        V1Namespace namespace = prepareNamespace("core");

        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("core"));
    }

    @Test
    public void namespace_with_two_suffixes() {
        EnvironmentResolverStrategyByParsingNamespaceName strategy = new EnvironmentResolverStrategyByParsingNamespaceName();
        V1Namespace namespace = prepareNamespace("env-core-bss");
        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("env-core"));
    }

    @Test
    public void namespace_without_metadata() {
        EnvironmentResolverStrategyByParsingNamespaceName strategy = new EnvironmentResolverStrategyByParsingNamespaceName();
        V1Namespace namespace = new V1Namespace();
        Assertions.assertThrows(IllegalStateException.class, () -> strategy.resolveEnvironmentName(namespace));
    }


}
