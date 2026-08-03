package br.com.itbn.sisdent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record LoginRequest(
        @Email @NotBlank
        @Schema(example = "admin@sisdent.local")
        String email,
        @NotBlank
        @Schema(example = "admin")
        String password) {
}
