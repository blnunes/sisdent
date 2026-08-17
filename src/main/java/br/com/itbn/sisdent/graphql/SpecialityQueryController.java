package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.filter.SpecialityFilter;
import br.com.itbn.sisdent.service.SpecialityService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL transport adapter for speciality reads. It contains no domain or persistence logic. */
@Controller
public class SpecialityQueryController {
    private final SpecialityService specialityService;
    private final CatalogueLocaleArgument catalogueLocale;

    public SpecialityQueryController(SpecialityService specialityService, CatalogueLocaleArgument catalogueLocale) {
        this.specialityService = specialityService;
        this.catalogueLocale = catalogueLocale;
    }

    @QueryMapping
    public PageResponse<SpecialityResponse> specialities(
            @Argument CataloguePageInput page,
            @Argument SpecialityFilterInput filter,
            @Argument String locale) {
        return specialityService.findPage(
                page == null ? new CataloguePageInput(null, null, null, null).toPageQuery() : page.toPageQuery(),
                filter == null ? new SpecialityFilter(null, null) : filter.toFilter(),
                catalogueLocale.resolve(locale));
    }
}
