package br.com.itbn.sisdent.dto;

import java.time.Instant;
import java.util.UUID;

/** Opaque management record; calendar rendering continues to use availability intervals only. */
public record AppointmentBlockedPeriodResponse(
        UUID globalId,
        UUID clinicUnitId,
        UUID practitionerId,
        Instant startAt,
        Instant endAt,
        long version) {
}
