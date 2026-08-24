package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PerformedProcedureRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Typed GraphQL input for recording a procedure performed during an appointment. */
public record PerformedProcedureMutationInput(
        @NotNull Long dentalProcedureId,
        @NotBlank String performedAt,
        @Size(max = 500) String administrativeNote) {

    PerformedProcedureRequest toRequest() {
        try {
            return new PerformedProcedureRequest(dentalProcedureId, Instant.parse(performedAt), administrativeNote);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
