package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdministrativeDivisionRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 32) String type,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode) {
}
