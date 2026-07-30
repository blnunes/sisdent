package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.IdentificationType;

import java.time.LocalDate;

/** Criteria used to restrict a patient collection independently from pagination. */
public record PatientFilter(
        Long id,
        String name,
        LocalDate birthDate,
        Boolean active,
        Gender gender,
        String taxId,
        IdentificationType identificationType,
        String identificationNumber,
        String nationalityCode,
        Long addressId,
        Long specialityId) {

    public String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }
}
