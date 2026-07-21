package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StateRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String abbreviation) {
}
