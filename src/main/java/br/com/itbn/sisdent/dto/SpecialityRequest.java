package br.com.itbn.sisdent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SpecialityRequest(
        @NotBlank String name,
        @NotEmpty List<@NotNull @Valid ProcedureRequest> procedures) {
}
