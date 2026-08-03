package br.com.itbn.sisdent.dto;

public record AdministrativeDivisionResponse(
        Long id,
        String name,
        String code,
        String type,
        CountryResponse country) {
}
