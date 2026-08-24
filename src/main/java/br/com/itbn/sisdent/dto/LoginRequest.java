package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record LoginRequest(
        @Email @NotBlank
        String email,
        @NotBlank
        String password) {
}
