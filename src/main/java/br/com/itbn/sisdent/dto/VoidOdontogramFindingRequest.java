package br.com.itbn.sisdent.dto; import jakarta.validation.constraints.*; public record VoidOdontogramFindingRequest(@NotBlank @Size(max=500) String reason,@NotNull Long version) {}
