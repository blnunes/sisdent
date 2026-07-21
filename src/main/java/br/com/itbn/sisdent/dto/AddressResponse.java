package br.com.itbn.sisdent.dto;

public record AddressResponse(
        Long id,
        String street,
        String district,
        String additionalInfo,
        String block,
        String postalCode,
        StateResponse state) {
}
