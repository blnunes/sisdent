package br.com.itbn.sisdent.dto;

import java.time.Instant;
import java.util.UUID;

/** Safe scheduling availability result for a practitioner interval. */
public record AppointmentAvailabilityResponse(UUID practitionerId, Instant startAt, Instant endAt,
        Availability availability, Category category) {
    public enum Availability {
        AVAILABLE,
        UNAVAILABLE
    }

    public enum Category {
        WORKING_HOURS,
        BREAK,
        BLOCKED,
        OCCUPIED
    }
}
