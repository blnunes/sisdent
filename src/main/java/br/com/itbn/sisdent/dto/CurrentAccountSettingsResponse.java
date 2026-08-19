package br.com.itbn.sisdent.dto;

import java.util.UUID;

public record CurrentAccountSettingsResponse(UUID id, String displayName, String email, long version) {
}
