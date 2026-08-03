package br.com.itbn.sisdent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank String street,
        String district,
        @NotBlank String city,
        String additionalInfo,
        String block,
        @Size(max = 20) String postalCode,
        @Valid AdministrativeDivisionReference administrativeDivision,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode) {

    public record AdministrativeDivisionReference(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 32) String type) {
    }
}
