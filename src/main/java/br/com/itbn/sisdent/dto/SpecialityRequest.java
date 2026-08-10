package br.com.itbn.sisdent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SpecialityRequest(
        @NotBlank String name,
        @NotEmpty List<@NotNull @Valid DentalProcedureRequest> procedures,
        Map<String, String> translations) {

    public SpecialityRequest(String name, List<DentalProcedureRequest> procedures) {
        this(name, procedures, Map.of());
    }

    public SpecialityRequest {
        translations = translations == null ? Map.of() : Map.copyOf(translations);
    }
}
