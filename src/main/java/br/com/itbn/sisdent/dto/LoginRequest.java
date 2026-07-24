package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.IdentificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull IdentificationType identificationType,
        @NotBlank String identificationNumber,
        @NotBlank String password) {
}
