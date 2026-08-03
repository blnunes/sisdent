package br.com.itbn.sisdent.dto;

import java.util.UUID;

public record OrganizationResponse(UUID id, String name, boolean active) {
}
