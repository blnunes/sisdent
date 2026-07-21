package br.com.itbn.sisdent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank String district,
        String additionalInfo,
        String block,
        @NotBlank @Pattern(regexp = "\\d{8}") String postalCode,
        @NotNull @Valid StateRequest state) {
}
