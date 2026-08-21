package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AdministrativeDivisionService administrativeDivisionService;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private AddressService addressService;

    @Test
    void returnsAddressesSortedByStreet() {
        when(addressRepository.findAll(Sort.by("street"))).thenReturn(List.of(address()));

        List<AddressResponse> responses = addressService.findAll();

        assertThat(responses).singleElement()
                .extracting(AddressResponse::postalCode)
                .isEqualTo("1250-096");
    }

    @Test
    void postalCodeLookupIsCountryScopedAndCanReturnMultipleAddresses() {
        when(addressRepository.findAllByCountry_CodeAndPostalCodeOrderByStreet("PT", "1250-096"))
                .thenReturn(List.of(address(), address()));

        List<AddressResponse> responses = addressService.findByPostalCode("PT", "1250-096");

        assertThat(responses).hasSize(2);
    }

    @Test
    void reusesAnExistingPatientAddressWithTheSameNormalizedDetails() {
        AddressRequest request = request();
        Country country = country();
        AdministrativeDivision division = division(country);
        when(countryService.requireByCode("PT")).thenReturn(country);
        when(administrativeDivisionService.findOrCreate(request.administrativeDivision(), country))
                .thenReturn(division);
        Address existing = address();
        when(addressRepository.findAllByCountry_CodeAndPostalCodeOrderByStreet("PT", "1250-096"))
                .thenReturn(List.of(existing));

        Address result = addressService.resolvePatientAddress(null, request);

        assertThat(result).isSameAs(existing);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void reusesTheAddressSelectedById() {
        Address existing = address();
        when(addressRepository.findById(42L)).thenReturn(java.util.Optional.of(existing));

        assertThat(addressService.resolvePatientAddress(42L, null)).isSameAs(existing);
    }

    @Test
    void suggestsPostalCodesWithinTheSelectedCountry() {
        when(addressRepository.findTop10ByCountry_CodeAndPostalCodeStartingWithOrderByPostalCodeAscStreetAsc("PT", "125"))
                .thenReturn(List.of(address()));

        List<AddressResponse> results = addressService.suggestByPostalCode("pt", "125");

        assertThat(results).singleElement().extracting(AddressResponse::postalCode).isEqualTo("1250-096");
    }

    @Test
    void createsUpdatesAndDeletesAddresses() {
        Address existing = address();
        AddressRequest request = request();
        Country country = country();
        when(countryService.requireByCode("PT")).thenReturn(country);
        when(administrativeDivisionService.findOrCreate(request.administrativeDivision(), country))
                .thenReturn(division(country));
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(addressRepository.existsById(1L)).thenReturn(true);

        assertThat(addressService.create(request).street()).isEqualTo("Avenida da Liberdade 100");
        assertThat(addressService.update(1L, request).city()).isEqualTo("Lisbon");
        addressService.delete(1L);
        verify(addressRepository).deleteById(1L);
    }

    @Test
    void rejectsInvalidSuggestionsAndInvalidAddressSelections() {
        assertThat(addressService.suggestByPostalCode("P", "125")).isEmpty();
        assertThat(addressService.suggestByPostalCode("PT", "1")).isEmpty();
        AddressRequest addressRequest = request();
        assertThatThrownBy(() -> addressService.resolvePatientAddress(1L, addressRequest))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("either addressId");
        assertThatThrownBy(() -> addressService.resolvePatientAddress(null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("required");
    }

    @Test
    void reportsMissingAddressesForUpdateDeleteAndSelection() {
        when(addressRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        when(addressRepository.existsById(2L)).thenReturn(false);

        AddressRequest addressRequest = request();
        assertThatThrownBy(() -> addressService.update(1L, addressRequest)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> addressService.delete(2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> addressService.resolvePatientAddress(1L, null)).isInstanceOf(ResponseStatusException.class);
    }

    private AddressRequest request() {
        return new AddressRequest(
                "Avenida da Liberdade 100",
                null,
                "Lisbon",
                "Floor 2",
                null,
                "1250-096",
                new AddressRequest.AdministrativeDivisionReference("Lisbon", "11", "DISTRICT"),
                "PT");
    }

    private Address address() {
        Country country = country();
        return new Address(Address.builder()
                .street("Avenida da Liberdade 100")
                .city("Lisbon")
                .additionalInfo("Floor 2")
                .postalCode("1250-096")
                .administrativeDivision(division(country))
                .country(country));
    }

    private AdministrativeDivision division(Country country) {
        return new AdministrativeDivision("Lisbon", "11", "DISTRICT", country);
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
