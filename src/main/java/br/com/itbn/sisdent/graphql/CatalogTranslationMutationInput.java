package br.com.itbn.sisdent.graphql;

import jakarta.validation.constraints.NotBlank;

/** One localized catalogue label, represented explicitly rather than as a JSON map. */
public record CatalogTranslationMutationInput(@NotBlank String locale, @NotBlank String value) {
}
