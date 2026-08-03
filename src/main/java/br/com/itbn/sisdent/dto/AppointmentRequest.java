package br.com.itbn.sisdent.dto; import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public record AppointmentRequest(@NotNull UUID clinicUnitId,@NotNull UUID patientId,@NotNull UUID practitionerId,@NotNull Instant startAt,@NotNull Instant endAt,@NotBlank @Size(max=64) String schedulingTimezone) {}
