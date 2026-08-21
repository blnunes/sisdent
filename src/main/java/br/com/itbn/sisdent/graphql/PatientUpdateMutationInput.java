package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Selected patient update only; versioned/destructive patient workflows remain REST-only. */
public record PatientUpdateMutationInput(
        @NotBlank String name, @NotBlank String birthDate, @NotNull Boolean active, @NotNull Gender gender,
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 -]{2,31}") String taxId,
        @NotNull DocumentType identificationType,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 -]{2,63}") String identificationNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String documentIssuerCountryCode,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String nationalityCode,
        Long addressId, @Valid PatientAddressMutationInput address,
        @NotNull Set<@NotNull Long> specialityIds) {
    PatientRequest toRequest() {
        try {
            return new PatientRequest(name, LocalDate.parse(birthDate), active, gender, taxId, identificationType,
                    identificationNumber, documentIssuerCountryCode, nationalityCode, addressId,
                    address == null ? null : address.toRequest(), specialityIds);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
