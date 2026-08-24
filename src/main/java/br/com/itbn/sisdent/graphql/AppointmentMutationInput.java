package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AppointmentRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Typed GraphQL input for appointment creation and rescheduling. */
public record AppointmentMutationInput(
        @NotNull UUID clinicUnitId,
        @NotNull UUID patientId,
        @NotNull UUID practitionerId,
        @NotBlank String startAt,
        @NotBlank String endAt,
        @NotBlank String schedulingTimezone) {

    AppointmentRequest toRequest() {
        try {
            return new AppointmentRequest(
                    clinicUnitId,
                    patientId,
                    practitionerId,
                    Instant.parse(startAt),
                    Instant.parse(endAt),
                    schedulingTimezone);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
