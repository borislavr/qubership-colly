package org.qubership.colly;

import io.kubernetes.client.openapi.models.V1Namespace;

public interface EnvironmentResolverStrategy {
    String resolveEnvironmentName(V1Namespace namespace);
}
