package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.repository.AdministrativeDivisionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrativeDivisionServiceTest {

    @Mock
    private AdministrativeDivisionRepository repository;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private AdministrativeDivisionService service;

    @Test
    void reusesDivisionOnlyWithinTheRequestedCountry() {
        Country portugal = country();
        AdministrativeDivision existing = new AdministrativeDivision(
                "Lisbon", "11", "DISTRICT", portugal);
        AddressRequest.AdministrativeDivisionReference request =
                new AddressRequest.AdministrativeDivisionReference("Lisbon", "11", "DISTRICT");
        when(repository.findByCountry_CodeAndCode("PT", "11")).thenReturn(Optional.of(existing));

        AdministrativeDivision result = service.findOrCreate(request, portugal);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsAddressWithoutAdministrativeDivision() {
        assertThat(service.findOrCreate(null, country())).isNull();
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
