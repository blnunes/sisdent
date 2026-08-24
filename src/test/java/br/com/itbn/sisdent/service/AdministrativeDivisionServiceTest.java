package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.AdministrativeDivisionRequest;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.repository.AdministrativeDivisionRepository;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private ScopeAuthorizationService authorization;

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

    @Test
    void createsDivisionWhenTheCountryDoesNotAlreadyContainItsCode() {
        Country portugal = country();
        var request = new AddressRequest.AdministrativeDivisionReference("Lisbon", " lx ", " district ");
        when(repository.findByCountry_CodeAndCode("PT", "LX")).thenReturn(Optional.empty());
        when(repository.save(any(AdministrativeDivision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdministrativeDivision division = service.findOrCreate(request, portugal);

        assertThat(division.getCode()).isEqualTo("LX");
        assertThat(division.getType()).isEqualTo("DISTRICT");
    }

    @Test
    void createsUpdatesAndDeletesAdministrativeDivisions() {
        Country portugal = country();
        AdministrativeDivision division = new AdministrativeDivision("Old", "OLD", "DISTRICT", portugal);
        AdministrativeDivisionRequest request = new AdministrativeDivisionRequest(" Lisbon ", " lx ", " district ", "PT");
        when(countryService.requireByCode("PT")).thenReturn(portugal);
        when(repository.saveAndFlush(any(AdministrativeDivision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(1L)).thenReturn(Optional.of(division));
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(service.create(request).code()).isEqualTo("LX");
        assertThat(service.update(1L, request).name()).isEqualTo("Lisbon");
        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void reportsMissingDivisionsForUpdateAndDelete() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.existsById(2L)).thenReturn(false);

        AdministrativeDivisionRequest request = new AdministrativeDivisionRequest("Lisbon", "LX", "DISTRICT", "PT");
        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
