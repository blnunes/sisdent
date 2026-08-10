package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CatalogTranslationRequest(
        @NotNull @Size(max = 3) Map<String, String> translations) {
}
