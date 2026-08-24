package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PatientLinkRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public record PatientLinkMutationInput(@NotNull DocumentType documentType, @NotBlank String issuerCountryCode,
        @NotBlank String documentNumber, @NotBlank String birthDate, UUID clinicUnitId,
        @NotNull PatientLinkBasis operationalBasis) {
    PatientLinkRequest toRequest() {
        return new PatientLinkRequest(documentType, issuerCountryCode, documentNumber, parseBirthDate(), clinicUnitId,
                operationalBasis);
    }

    private LocalDate parseBirthDate() {
        try {
            return LocalDate.parse(birthDate);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
