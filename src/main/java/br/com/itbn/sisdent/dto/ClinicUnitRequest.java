package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicUnitRequest(@NotBlank @Size(max = 255) String name) {
}
