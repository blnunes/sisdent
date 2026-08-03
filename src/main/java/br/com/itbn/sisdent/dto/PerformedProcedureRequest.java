package br.com.itbn.sisdent.dto; import jakarta.validation.constraints.*; import java.time.*;
public record PerformedProcedureRequest(@NotNull Long dentalProcedureId,@NotNull Instant performedAt,@Size(max=500) String administrativeNote) {}
