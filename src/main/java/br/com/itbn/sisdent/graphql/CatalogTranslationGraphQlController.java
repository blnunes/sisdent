package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CatalogTranslationEntryResponse;
import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.service.CatalogTranslationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class CatalogTranslationGraphQlController {
    private final CatalogTranslationService translations;

    public CatalogTranslationGraphQlController(CatalogTranslationService translations) {
        this.translations = translations;
    }

    @QueryMapping
    public List<CatalogTranslationEntry> catalogTranslations(@Argument CatalogResourceType type, @Argument String query) {
        return translations.findAll(type, query).stream().map(CatalogTranslationEntry::from).toList();
    }

    @MutationMapping
    public CatalogTranslationEntry replaceCatalogTranslations(
            @Argument CatalogResourceType type, @Argument Long id,
            @Argument @Valid List<CatalogTranslationMutationInput> translations) {
        return CatalogTranslationEntry.from(this.translations.replace(type, id, translations.stream()
                .collect(java.util.stream.Collectors.toMap(CatalogTranslationMutationInput::locale,
                        CatalogTranslationMutationInput::value))));
    }

    public record CatalogTranslationEntry(
            CatalogResourceType resourceType, Long resourceId, Long parentId, String canonicalName,
            List<CatalogTranslationValue> translations, List<String> customizedLocales, List<String> missingLocales) {
        static CatalogTranslationEntry from(CatalogTranslationEntryResponse source) {
            return new CatalogTranslationEntry(source.resourceType(), source.resourceId(), source.parentId(),
                    source.canonicalName(), source.translations().entrySet().stream()
                            .map(entry -> new CatalogTranslationValue(entry.getKey(), entry.getValue())).toList(),
                    source.customizedLocales().stream().sorted().toList(), source.missingLocales().stream().sorted().toList());
        }
    }

    public record CatalogTranslationValue(String locale, String value) { }
}
