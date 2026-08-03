package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotNull;

public record AccountPlatformAdministratorRequest(
        boolean platformAdministrator,
        @NotNull Long version) {
}
