package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.OdontogramFindingCreateRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.OdontogramCondition;
import br.com.itbn.sisdent.model.OdontogramSurface;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Typed GraphQL input for immutable odontogram observations. */
public record OdontogramFindingMutationInput(
        @NotNull UUID clinicUnitId,
        @NotNull UUID patientId,
        UUID practitionerId,
        UUID replacementForId,
        @NotBlank String toothCode,
        @NotNull OdontogramSurface surface,
        @NotNull OdontogramCondition condition,
        @NotBlank String observedAt,
        @NotBlank @Size(max = 64) String observationTimezone,
        @Size(max = 500) String clinicalNote) {

    OdontogramFindingCreateRequest toRequest() {
        try {
            return new OdontogramFindingCreateRequest(clinicUnitId, patientId, practitionerId, replacementForId,
                    toothCode, surface, condition, Instant.parse(observedAt), observationTimezone, clinicalNote);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
