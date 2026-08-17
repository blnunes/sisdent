package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.localization.CatalogNameLocalizer;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {
    @Mock CountryRepository countries;
    @Mock PageableFactory pageableFactory;
    @Mock CatalogNameLocalizer<Country> nameLocalizer;
    @InjectMocks CountryService service;

    @Test
    void listsAndResolvesCountriesByCode() {
        Country portugal = country();
        when(countries.findAll(org.springframework.data.domain.Sort.by("name"))).thenReturn(List.of(portugal));
        when(countries.findByCode("PT")).thenReturn(Optional.of(portugal));
        when(nameLocalizer.localize(portugal, Locale.ENGLISH)).thenReturn("Portugal");

        assertThat(service.findAll()).singleElement().extracting(response -> response.code()).isEqualTo("PT");
        assertThat(service.requireByCode("PT")).isSameAs(portugal);
        assertThatThrownBy(() -> service.requireByCode("ZZ")).isInstanceOf(UnknownCountryException.class);
    }

    @Test
    void createsUpdatesAndDeletesCountries() {
        Country portugal = country();
        CountryRequest request = new CountryRequest("Portugal", "PT", Continent.EUROPE);
        when(countries.saveAndFlush(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(countries.findById(1L)).thenReturn(Optional.of(portugal));
        when(countries.existsById(1L)).thenReturn(true);

        when(nameLocalizer.localize(any(Country.class), org.mockito.ArgumentMatchers.eq(Locale.forLanguageTag("pt-PT"))))
                .thenReturn("Portugal");

        assertThat(service.create(request, Locale.forLanguageTag("pt-PT")).displayName()).isEqualTo("Portugal");
        assertThat(service.update(1L, request, Locale.forLanguageTag("pt-PT")).code()).isEqualTo("PT");
        service.delete(1L);
        verify(countries).deleteById(1L);
    }

    @Test
    void returnsCanonicalAndLocalizedNamesOnPagedResponses() {
        Country netherlands = new Country("Netherlands", "NL", Continent.EUROPE);
        PageRequest pageable = PageRequest.of(0, 10);
        when(pageableFactory.create(any(), any())).thenReturn(pageable);
        when(countries.findAll(pageable)).thenReturn(new PageImpl<>(List.of(netherlands), pageable, 1));
        when(nameLocalizer.localize(netherlands, Locale.forLanguageTag("nl"))).thenReturn("Nederland");

        var response = service.findPage(new br.com.itbn.sisdent.pagination.PageQuery(0, 10, "name", "asc"),
                Locale.forLanguageTag("nl")).content().getFirst();

        assertThat(response.name()).isEqualTo("Netherlands");
        assertThat(response.displayName()).isEqualTo("Nederland");
    }

    @Test
    void rejectsMutationsForUnknownCountries() {
        when(countries.findById(1L)).thenReturn(Optional.empty());
        when(countries.existsById(2L)).thenReturn(false);
        CountryRequest request = new CountryRequest("Portugal", "PT", Continent.EUROPE);

        assertThatThrownBy(() -> service.update(1L, request, Locale.ENGLISH))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(exception -> ((ResourceNotFoundException) exception).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(exception -> ((ResourceNotFoundException) exception).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void rejectsUnsupportedCatalogueLocaleWithoutPassingItToTheLocalizer() {
        assertThatThrownBy(() -> service.findPage(new br.com.itbn.sisdent.pagination.PageQuery(0, 10, "name", "asc"),
                Locale.forLanguageTag("zh-CN")))
                .isInstanceOf(ValidationException.class)
                .extracting(exception -> ((ValidationException) exception).errorCode())
                .isEqualTo(ErrorCode.CATALOG_UNSUPPORTED_LOCALE);
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
