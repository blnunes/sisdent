package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record DentalProcedureRequest(
        Long id,
        @NotBlank String name,
        Map<String, String> translations) {

    public DentalProcedureRequest(Long id, String name) {
        this(id, name, Map.of());
    }

    public DentalProcedureRequest {
        translations = translations == null ? Map.of() : Map.copyOf(translations);
    }
}
