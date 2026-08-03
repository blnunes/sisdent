package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.CatalogStatus;
import java.util.List;

public record SpecialityResponse(
        Long id,
        String name,
        CatalogStatus status,
        List<DentalProcedureResponse> procedures) {
}
