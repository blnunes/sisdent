package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.AdministrativeDivisionResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Speciality;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMapperTest {

    @Test
    void mapsAdministrativeDivisionWithCountry() {
        Country country = country();

        AdministrativeDivisionResponse response = ResponseMapper.toResponse(
                new AdministrativeDivision("Lisbon", "11", "DISTRICT", country));

        assertThat(response.name()).isEqualTo("Lisbon");
        assertThat(response.code()).isEqualTo("11");
        assertThat(response.country().code()).isEqualTo("PT");
    }

    @Test
    void mapsInternationalAddress() {
        Country country = country();
        Address address = new Address(Address.builder()
                .street("Avenida da Liberdade 100")
                .city("Lisbon")
                .additionalInfo("Floor 2")
                .postalCode("1250-096")
                .administrativeDivision(new AdministrativeDivision("Lisbon", "11", "DISTRICT", country))
                .country(country));

        AddressResponse response = ResponseMapper.toResponse(address);

        assertThat(response.city()).isEqualTo("Lisbon");
        assertThat(response.postalCode()).isEqualTo("1250-096");
        assertThat(response.administrativeDivision().code()).isEqualTo("11");
    }

    @Test
    void mapsPatientGlobalIdentityAndIssuedDocument() {
        Country country = country();
        Address address = new Address(Address.builder()
                .street("Rua Augusta 1").city("Lisbon").postalCode("1100-053").country(country));
        Patient patient = new Patient(new Patient.PatientDetails(
                new Patient.PatientIdentity(
                        "Ana Silva",
                        LocalDate.of(1992, 4, 18),
                        true,
                        Gender.FEMALE,
                        null),
                new Patient.PatientDocument(
                        DocumentType.NATIONAL_ID_CARD,
                        "12345ABC",
                        country,
                        country),
                address,
                List.of(new Speciality("Orthodontics"))));

        PatientResponse response = ResponseMapper.toResponse(patient);

        assertThat(response.globalId()).isNotNull();
        assertThat(response.identificationType()).isEqualTo(DocumentType.NATIONAL_ID_CARD);
        assertThat(response.documentIssuerCountry().code()).isEqualTo("PT");
        assertThat(response.address().administrativeDivision()).isNull();
        assertThat(response.specialities()).extracting(SpecialityResponse::name)
                .containsExactly("Orthodontics");
    }

    private Country country() {
        return new Country("Portugal", "PT", Continent.EUROPE);
    }
}
