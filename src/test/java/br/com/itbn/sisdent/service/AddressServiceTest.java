package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

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
