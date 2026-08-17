package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.localization.SupportedCatalogLocale;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.CountryService;
import java.util.Locale;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL transport adapter for the country catalogue.
 *
 * <p>This controller deliberately contains no authorization, persistence, or domain logic.
 * Future GraphQL resolvers should follow the same pattern and reuse their existing services.</p>
 */
@Controller
public class CountryQueryController {

    private final CountryService countryService;

    public CountryQueryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @QueryMapping
    public PageResponse<CountryResponse> countries(
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sort,
            @Argument String direction,
            @Argument String locale) {
        return countryService.findPage(new PageQuery(page, size, sort, direction), requestedLocale(locale));
    }

    @QueryMapping
    public CountryResponse country(@Argument String code, @Argument String locale) {
        return countryService.findByCode(code, requestedLocale(locale));
    }

    private Locale requestedLocale(String locale) {
        Locale requestedLocale = locale == null || locale.isBlank()
                ? Locale.ENGLISH
                : Locale.forLanguageTag(locale);
        if (!SupportedCatalogLocale.supports(requestedLocale)) {
            throw new ValidationException(ErrorCode.CATALOG_UNSUPPORTED_LOCALE,
                    Map.of("locale", locale, "supportedLocales", SupportedCatalogLocale.supportedLanguageTags()));
        }
        return requestedLocale;
    }
}
