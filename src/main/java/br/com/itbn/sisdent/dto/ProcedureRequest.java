package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcedureRequest(
        Long id,
        @NotBlank String name) {
}
