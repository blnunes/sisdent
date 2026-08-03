package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.DocumentType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientResponse(
        Long id,
        UUID globalId,
        String name,
        LocalDate birthDate,
        boolean active,
        Gender gender,
        String taxId,
        DocumentType identificationType,
        String identificationNumber,
        CountryResponse documentIssuerCountry,
        CountryResponse nationality,
        AddressResponse address,
        List<SpecialityResponse> specialities) {
}
