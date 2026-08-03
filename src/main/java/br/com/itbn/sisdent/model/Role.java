package br.com.itbn.sisdent.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    MANAGER(EnumSet.of(
            Permission.READ_PATIENTS,
            Permission.MAINTAIN_PATIENTS,
            Permission.READ_SPECIALITIES,
            Permission.MAINTAIN_SPECIALITIES,
            Permission.READ_ADDRESSES,
            Permission.READ_COUNTRIES,
            Permission.READ_ADMINISTRATIVE_DIVISIONS)),
    USER(EnumSet.of(
            Permission.READ_PATIENTS,
            Permission.READ_SPECIALITIES,
            Permission.READ_ADDRESSES,
            Permission.READ_COUNTRIES,
            Permission.READ_ADMINISTRATIVE_DIVISIONS));

    private final Set<Permission> defaultPermissions;

    Role(Set<Permission> defaultPermissions) {
        this.defaultPermissions = Set.copyOf(defaultPermissions);
    }

    public Set<Permission> defaultPermissions() {
        return defaultPermissions;
    }
}
