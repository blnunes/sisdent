package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {
    @Mock CountryRepository countries;
    @Mock PageableFactory pageableFactory;
    @InjectMocks CountryService service;

    @Test
    void listsAndResolvesCountriesByCode() {
        Country portugal = country();
        when(countries.findAll(org.springframework.data.domain.Sort.by("name"))).thenReturn(List.of(portugal));
        when(countries.findByCode("PT")).thenReturn(Optional.of(portugal));

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

        assertThat(service.create(request).name()).isEqualTo("Portugal");
        assertThat(service.update(1L, request).code()).isEqualTo("PT");
        service.delete(1L);
        verify(countries).deleteById(1L);
    }

    @Test
    void rejectsMutationsForUnknownCountries() {
        when(countries.findById(1L)).thenReturn(Optional.empty());
        when(countries.existsById(2L)).thenReturn(false);
        CountryRequest request = new CountryRequest("Portugal", "PT", Continent.EUROPE);

        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(ResponseStatusException.class);
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
