package br.com.itbn.sisdent.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    MANAGER(EnumSet.allOf(Permission.class)),
    USER(EnumSet.of(Permission.READ));

    private final Set<Permission> defaultPermissions;

    Role(Set<Permission> defaultPermissions) {
        this.defaultPermissions = Set.copyOf(defaultPermissions);
    }

    public Set<Permission> defaultPermissions() {
        return defaultPermissions;
    }
}
