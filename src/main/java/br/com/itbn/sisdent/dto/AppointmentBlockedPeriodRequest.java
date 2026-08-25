package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Validated command for an organization- and clinic-scoped blocked period. */
public record AppointmentBlockedPeriodRequest(
        @NotNull UUID clinicUnitId,
        UUID practitionerId,
        @NotNull Instant startAt,
        @NotNull Instant endAt) {
}
