package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private StateService stateService;

    @InjectMocks
    private AddressService addressService;

    @Test
    void returnsAddressesSortedByStreet() {
        Address address = address("01310100");
        when(addressRepository.findAll(Sort.by("street"))).thenReturn(List.of(address));

        List<AddressResponse> responses = addressService.findAll();

        assertThat(responses).singleElement()
                .extracting(AddressResponse::postalCode)
                .isEqualTo("01310100");
        verify(addressRepository).findAll(Sort.by("street"));
    }

    @Test
    void returnsEmptyWhenPostalCodeDoesNotExist() {
        when(addressRepository.findByPostalCode("00000000")).thenReturn(Optional.empty());

        Optional<AddressResponse> response = addressService.findByPostalCode("00000000");

        assertThat(response).isEmpty();
    }

    @Test
    void reusesExistingAddress() {
        Address existingAddress = address("01310100");
        AddressRequest request = addressRequest();
        when(addressRepository.findByPostalCode(request.postalCode()))
                .thenReturn(Optional.of(existingAddress));

        Address result = addressService.findOrCreate(request);

        assertThat(result).isSameAs(existingAddress);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void createsMissingAddressWithResolvedState() {
        AddressRequest request = addressRequest();
        State state = new State("São Paulo", "SP");
        when(addressRepository.findByPostalCode(request.postalCode())).thenReturn(Optional.empty());
        when(stateService.findOrCreate(request.state())).thenReturn(state);
        when(addressRepository.save(any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.findOrCreate(request);

        assertThat(result.getState()).isSameAs(state);
        verify(addressRepository).save(any(Address.class));
    }

    private AddressRequest addressRequest() {
        return new AddressRequest(
                "Avenida Paulista",
                "Bela Vista",
                "Suite 1204",
                "B",
                "01310100",
                new StateRequest("São Paulo", "SP"));
    }

    private Address address(String postalCode) {
        return new Address(
                "Avenida Paulista",
                "Bela Vista",
                "Suite 1204",
                "B",
                postalCode,
                new State("São Paulo", "SP"));
    }
}
