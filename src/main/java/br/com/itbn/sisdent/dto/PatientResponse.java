package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Gender;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String name,
        LocalDate birthDate,
        boolean active,
        Gender gender,
        String taxId,
        AddressResponse address) {
}
