package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AddressService addressService;

    @InjectMocks
    private PatientService patientService;

    @Test
    void returnsPatientsSortedByName() {
        when(patientRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(patient(existingAddress())));

        List<PatientResponse> responses = patientService.findAll();

        assertThat(responses).singleElement()
                .extracting(PatientResponse::name)
                .isEqualTo("Ana Souza");
        verify(patientRepository).findAll(Sort.by("name"));
    }

    @Test
    void returnsEmptyWhenPatientDoesNotExist() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<PatientResponse> response = patientService.findById(999L);

        assertThat(response).isEmpty();
    }

    @Test
    void createsPatientReusingExistingAddress() {
        Address address = existingAddress();
        when(addressService.findOrCreate(patientRequest().address())).thenReturn(address);
        when(patientRepository.save(any(Patient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.create(patientRequest());

        assertThat(response.name()).isEqualTo("Ana Souza");
        assertThat(response.address().postalCode()).isEqualTo("01310100");
        verify(addressService).findOrCreate(patientRequest().address());
    }

    @Test
    void createsPatientWithResolvedAddress() {
        Address address = existingAddress();
        when(addressService.findOrCreate(patientRequest().address())).thenReturn(address);
        when(patientRepository.save(any(Patient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.create(patientRequest());

        assertThat(response.address().state().abbreviation()).isEqualTo("SP");
        verify(addressService).findOrCreate(patientRequest().address());
        verify(patientRepository).save(any(Patient.class));
    }

    private PatientRequest patientRequest() {
        return new PatientRequest(
                "Ana Souza",
                LocalDate.of(1992, Month.APRIL, 18),
                true,
                Gender.FEMALE,
                "12345678901",
                new AddressRequest(
                        "Avenida Paulista",
                        "Bela Vista",
                        "Suite 1204",
                        "B",
                        "01310100",
                        new StateRequest("São Paulo", "SP")));
    }

    private Patient patient(Address address) {
        return new Patient(
                "Ana Souza",
                LocalDate.of(1992, Month.APRIL, 18),
                true,
                Gender.FEMALE,
                "12345678901",
                address);
    }

    private Address existingAddress() {
        return new Address(
                "Avenida Paulista",
                "Bela Vista",
                "Suite 1204",
                "B",
                "01310100",
                new State("São Paulo", "SP"));
    }
}
