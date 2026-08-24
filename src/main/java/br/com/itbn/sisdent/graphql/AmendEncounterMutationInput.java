package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AmendEncounterRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Typed GraphQL input for an immutable finalized-encounter amendment. */
public record AmendEncounterMutationInput(
        @NotNull UUID clinicUnitId,
        UUID appointmentId,
        UUID practitionerId,
        @NotBlank String careAt,
        @NotBlank @Size(max = 64) String careTimezone,
        @NotBlank @Size(max = 4000) String narrative,
        @Size(max = 500) String administrativeNote,
        @NotBlank @Size(max = 500) String reason) {

    AmendEncounterRequest toRequest() {
        try {
            return new AmendEncounterRequest(clinicUnitId, appointmentId, practitionerId, Instant.parse(careAt),
                    careTimezone, narrative, administrativeNote, reason);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
