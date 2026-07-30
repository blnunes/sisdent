package br.com.itbn.sisdent.dto;

public record AddressResponse(
        Long id,
        String street,
        String district,
        String city,
        String additionalInfo,
        String block,
        String postalCode,
        AdministrativeDivisionResponse administrativeDivision,
        CountryResponse country) {
}
