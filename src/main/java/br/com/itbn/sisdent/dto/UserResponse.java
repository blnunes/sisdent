package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Permission;
import br.com.itbn.sisdent.model.Role;

import java.util.Set;

public record UserResponse(
        Long id,
        IdentificationType identificationType,
        String identificationNumber,
        Role role,
        Set<Permission> permissions,
        boolean active) {
}
