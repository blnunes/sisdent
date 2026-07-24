package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Permission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PermissionRequest(@NotNull Set<Permission> permissions) {
}
