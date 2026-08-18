package br.com.itbn.sisdent.graphql;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record DentalProcedureMutationInput(
        Long id,
        @NotBlank String name,
        List<@Valid CatalogTranslationMutationInput> translations) {
}
