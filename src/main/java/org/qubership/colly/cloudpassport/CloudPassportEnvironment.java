package org.qubership.colly.cloudpassport;

import java.util.Set;

public record CloudPassportEnvironment(String name, String description, Set<CloudPassportNamespace> namespaceDtos) {
}
