package br.com.itbn.sisdent.dto; import jakarta.validation.constraints.*;
public record VoidPerformedProcedureRequest(@NotBlank @Size(max=500) String reason) {}
