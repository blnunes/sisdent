package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.CatalogStatus;
import java.util.Map;

public record DentalProcedureResponse(Long id, String name, String displayName, CatalogStatus status,
                                      Map<String, String> translations) {
}
