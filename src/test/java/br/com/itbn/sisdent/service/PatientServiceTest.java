package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    @Mock
    private SpecialityService specialityService;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private PatientService patientService;

    @Test
    void createsGlobalPatientWithCountryScopedDocumentAndOptionalTaxId() {
        PatientRequest request = patientRequest();
        Country portugal = country();
        when(addressService.resolvePatientAddress(request.addressId(), request.address())).thenReturn(address(portugal));
        when(specialityService.findAllByIds(request.specialityIds()))
                .thenReturn(List.of(new Speciality("Orthodontics")));
        when(countryService.requireByCode("PT")).thenReturn(portugal);
        when(patientRepository.saveAndFlush(any(Patient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.create(request);

        assertThat(response.globalId()).isNotNull();
        assertThat(response.taxId()).isNull();
        assertThat(response.identificationType()).isEqualTo(DocumentType.NATIONAL_ID_CARD);
        assertThat(response.documentIssuerCountry().code()).isEqualTo("PT");
        assertThat(response.address().postalCode()).isEqualTo("1250-096");
        verify(addressService).resolvePatientAddress(request.addressId(), request.address());
    }

    private PatientRequest patientRequest() {
        return new PatientRequest(
                "Ana Silva",
                LocalDate.of(1992, 4, 18),
                true,
                Gender.FEMALE,
                null,
                DocumentType.NATIONAL_ID_CARD,
                "12 345-ABC",
                "PT",
                "PT",
                null,
                new AddressRequest(
                        "Avenida da Liberdade 100",
                        null,
                        "Lisbon",
                        null,
                        null,
                        "1250-096",
                        null,
                        "PT"),
                Set.of(1L));
    }

    private Address address(Country country) {
        return new Address(
                "Avenida da Liberdade 100",
                null,
                "Lisbon",
                null,
                null,
                "1250-096",
                null,
                country);
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
