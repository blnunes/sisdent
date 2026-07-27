package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.IdentificationType;

import java.time.LocalDate;
import java.util.List;

public record PatientResponse(
        Long id,
        String name,
        LocalDate birthDate,
        boolean active,
        Gender gender,
        String taxId,
        IdentificationType identificationType,
        String identificationNumber,
        CountryResponse nationality,
        AddressResponse address,
        List<SpecialityResponse> specialities) {
}
