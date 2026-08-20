package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOwnProfileRequest(@NotBlank String displayName, @NotNull Long version) {
}
