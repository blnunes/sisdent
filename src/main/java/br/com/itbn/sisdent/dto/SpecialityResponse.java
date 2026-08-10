package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.CatalogStatus;
import java.util.List;
import java.util.Map;

public record SpecialityResponse(
        Long id,
        String name,
        String displayName,
        CatalogStatus status,
        List<DentalProcedureResponse> procedures,
        Map<String, String> translations) {
}
