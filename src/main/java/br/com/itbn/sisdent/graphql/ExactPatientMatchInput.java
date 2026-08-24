package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ExactPatientMatchRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record ExactPatientMatchInput(@NotNull DocumentType documentType,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String issuerCountryCode, @NotBlank String documentNumber,
        @NotBlank String birthDate) {
    ExactPatientMatchRequest toRequest() {
        return new ExactPatientMatchRequest(documentType, issuerCountryCode, documentNumber, parseBirthDate());
    }

    private LocalDate parseBirthDate() {
        try {
            return LocalDate.parse(birthDate);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
