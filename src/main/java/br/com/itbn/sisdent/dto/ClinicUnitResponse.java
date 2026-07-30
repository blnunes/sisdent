package br.com.itbn.sisdent.dto;

import java.util.UUID;

public record ClinicUnitResponse(UUID id, UUID organizationId, String name, boolean active) {
}
