package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Typed GraphQL input for an opaque blocked-period management record. */
public record AppointmentBlockedPeriodMutationInput(
        @NotNull UUID clinicUnitId,
        UUID practitionerId,
        @NotBlank String startAt,
        @NotBlank String endAt) {

    AppointmentBlockedPeriodRequest toRequest() {
        try {
            return new AppointmentBlockedPeriodRequest(
                    clinicUnitId,
                    practitionerId,
                    Instant.parse(startAt),
                    Instant.parse(endAt));
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
