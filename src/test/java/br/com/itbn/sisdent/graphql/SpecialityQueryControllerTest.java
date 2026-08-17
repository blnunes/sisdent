package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.filter.SpecialityFilter;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.SpecialityService;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecialityQueryControllerTest {
    @Test
    void delegatesTypedPageFilterAndRegionalLocaleToExistingService() {
        SpecialityService service = mock(SpecialityService.class);
        PageResponse<SpecialityResponse> expected = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(service.findPage(new PageQuery(0, 10, "name", "ASC"), new SpecialityFilter("ortho", null),
                Locale.forLanguageTag("nl-BE"))).thenReturn(expected);
        SpecialityQueryController controller = new SpecialityQueryController(service, new CatalogueLocaleArgument());

        assertThat(controller.specialities(new CataloguePageInput(0, 10, "name", SortDirection.ASC),
                new SpecialityFilterInput("ortho", null), "nl-BE")).isSameAs(expected);

        verify(service).findPage(new PageQuery(0, 10, "name", "ASC"), new SpecialityFilter("ortho", null),
                Locale.forLanguageTag("nl-BE"));
    }
}
