package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ClinicalEncounterCreateRequest;
import br.com.itbn.sisdent.dto.ClinicalEncounterRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Typed GraphQL input for draft encounter creation and optimistic-lock updates. */
public record ClinicalEncounterMutationInput(
        @NotNull UUID clinicUnitId,
        @NotNull UUID patientId,
        UUID appointmentId,
        UUID practitionerId,
        @NotBlank String careAt,
        @NotBlank @Size(max = 64) String careTimezone,
        @NotBlank @Size(max = 4000) String narrative,
        @Size(max = 500) String administrativeNote,
        Long version) {

    ClinicalEncounterCreateRequest toCreateRequest() {
        return new ClinicalEncounterCreateRequest(clinicUnitId, patientId, appointmentId, practitionerId, instant(),
                careTimezone, narrative, administrativeNote);
    }

    ClinicalEncounterRequest toUpdateRequest() {
        if (version == null) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
        return new ClinicalEncounterRequest(clinicUnitId, patientId, appointmentId, practitionerId, instant(),
                careTimezone, narrative, administrativeNote, version);
    }

    private Instant instant() {
        try {
            return Instant.parse(careAt);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
