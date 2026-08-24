package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.service.CountryService;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CountryQueryControllerTest {

    @Test
    void delegatesPaginationAndRequestedLocaleToCountryService() {
        CountryService service = mock(CountryService.class);
        CountryQueryController controller = new CountryQueryController(service, new CatalogueLocaleArgument());
        PageResponse<CountryResponse> expected = new PageResponse<>(List.of(), 1, 5, 0, 0);
        when(service.findPage(any(), any())).thenReturn(expected);

        PageResponse<CountryResponse> actual = controller.countries(
                new CataloguePageInput(1, 5, "code", SortDirection.DESC), "pt-PT");

        assertThat(actual).isSameAs(expected);
        verify(service).findPage(new br.com.itbn.sisdent.pagination.PageQuery(1, 5, "code", "DESC"),
                Locale.forLanguageTag("pt-PT"));
    }

    @Test
    void rejectsBlankLocaleWithTheStableUnsupportedLocaleCode() {
        CountryService service = mock(CountryService.class);
        CountryQueryController controller = new CountryQueryController(service, new CatalogueLocaleArgument());
        assertThatThrownBy(() -> controller.countries(null, " "))
                .isInstanceOf(ValidationException.class)
                .extracting(exception -> ((ValidationException) exception).errorCode())
                .isEqualTo(ErrorCode.CATALOG_UNSUPPORTED_LOCALE);
    }

    @Test
    void rejectsAnUnsupportedLocaleWithItsStableErrorCode() {
        CountryService service = mock(CountryService.class);
        CountryQueryController controller = new CountryQueryController(service, new CatalogueLocaleArgument());
        CataloguePageInput pageInput = new CataloguePageInput(0, 10, "name", SortDirection.ASC);

        assertThatThrownBy(() -> controller.countries(pageInput, "zh-CN"))
                .isInstanceOf(ValidationException.class)
                .satisfies(throwable -> {
                    ValidationException exception = (ValidationException) throwable;
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.CATALOG_UNSUPPORTED_LOCALE);
                    assertThat(exception.getMessage()).isEqualTo("CATALOG.UNSUPPORTED_LOCALE");
                    assertThat(exception.safeMetadata())
                            .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("supportedLocales", "en, nl, pt"));
                });
    }
}
