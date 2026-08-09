package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.CatalogResourceType;
import java.util.Map;
import java.util.Set;

public record CatalogTranslationEntryResponse(
        CatalogResourceType resourceType,
        Long resourceId,
        Long parentId,
        String canonicalName,
        Map<String, String> translations,
        Set<String> customizedLocales,
        Set<String> missingLocales) {
}
