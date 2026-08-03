package br.com.itbn.sisdent.pagination;

import java.util.Set;

/** Defines the only persistent fields a collection may expose for ordering. */
public record SortDefinition(String defaultProperty, Set<String> allowedProperties) {

    public SortDefinition {
        allowedProperties = Set.copyOf(allowedProperties);
        if (!allowedProperties.contains(defaultProperty)) {
            throw new IllegalArgumentException("The default property must be allowed");
        }
    }
}
