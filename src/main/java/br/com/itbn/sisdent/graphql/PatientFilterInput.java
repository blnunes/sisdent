package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Typed search criteria for a scoped patient collection. */
public record PatientFilterInput(Long id, String name, String birthDate, Boolean active, Gender gender, String taxId,
        DocumentType identificationType, String identificationNumber, String nationalityCode, Long addressId,
        Long specialityId) {
    PatientFilter toFilter() {
        return new PatientFilter(id, name, parseBirthDate(), active, gender, taxId, identificationType,
                identificationNumber, nationalityCode, addressId, specialityId);
    }

    private LocalDate parseBirthDate() {
        if (birthDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(birthDate);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
