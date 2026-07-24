package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.IdentificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull
        @Schema(example = "NATIONAL_ID")
        IdentificationType identificationType,
        @NotBlank
        @Schema(example = "ADMIN")
        String identificationNumber,
        @NotBlank
        @Schema(example = "admin")
        String password) {
}
