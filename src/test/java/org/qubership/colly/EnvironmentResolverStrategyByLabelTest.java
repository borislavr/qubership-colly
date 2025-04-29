package org.qubership.colly;

import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.qubership.colly.EnvironmentResolverStrategyByLabel.ENVIRONMENT_NAME;

class EnvironmentResolverStrategyByLabelTest {
    private @NotNull V1Namespace prepareNamespace(String name) {
        V1Namespace namespace = new V1Namespace();
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(name);
        HashMap<String, String> labels = new HashMap<>();
        metadata.setLabels(labels);
        namespace.setMetadata(metadata);

        return namespace;
    }

    @Test
    public void resolve_namespace() {
        EnvironmentResolverStrategyByLabel strategy = new EnvironmentResolverStrategyByLabel();
        V1Namespace namespace = prepareNamespace("my-namespace");
        namespace.getMetadata().getLabels().put(ENVIRONMENT_NAME, "my-env");
        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("my-env"));
    }

    @Test
    void resolve_namespace_without_labels() {
        EnvironmentResolverStrategyByLabel strategy = new EnvironmentResolverStrategyByLabel();
        V1Namespace namespace = prepareNamespace("some-namespace");
        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("some-namespace"));
    }

    @Test
    void resolve_namespace_with_incorrect_labels() {
        EnvironmentResolverStrategyByLabel strategy = new EnvironmentResolverStrategyByLabel();
        V1Namespace namespace = prepareNamespace("my-namespace");
        namespace.getMetadata().getLabels().put("incorret_label", "my-env");
        String envName = strategy.resolveEnvironmentName(namespace);
        assertThat(envName, equalTo("my-namespace"));
    }

    @Test
    void resolve_namespace_without_name_and_labels() {
        EnvironmentResolverStrategyByLabel strategy = new EnvironmentResolverStrategyByLabel();
        V1Namespace namespace = new V1Namespace();
        Assertions.assertThrows(IllegalArgumentException.class, () -> strategy.resolveEnvironmentName(namespace));
    }


}
