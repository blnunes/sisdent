package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import java.util.List;
import br.com.itbn.sisdent.service.CountryService;
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
    private final CatalogueLocaleArgument catalogueLocale;

    public CountryQueryController(CountryService countryService, CatalogueLocaleArgument catalogueLocale) {
        this.countryService = countryService;
        this.catalogueLocale = catalogueLocale;
    }

    @QueryMapping
    public PageResponse<CountryResponse> countries(
            @Argument CataloguePageInput page,
            @Argument String locale) {
        return countryService.findPage(page == null ? new CataloguePageInput(null, null, null, null).toPageQuery()
                : page.toPageQuery(), catalogueLocale.resolve(locale));
    }

    @QueryMapping
    public CountryResponse country(@Argument String code, @Argument String locale) {
        return countryService.findByCode(code, catalogueLocale.resolve(locale));
    }

    @QueryMapping
    public List<br.com.itbn.sisdent.model.Continent> continents() {
        return countryService.continents();
    }
}
