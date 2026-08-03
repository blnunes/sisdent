package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.CatalogStatus;

public record DentalProcedureResponse(Long id, String name, CatalogStatus status) {
}
