package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.State;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMapperTest {

    @Test
    void mapsStateToResponse() {
        StateResponse response = ResponseMapper.toResponse(new State("Goiás", "GO"));

        assertThat(response.name()).isEqualTo("Goiás");
        assertThat(response.abbreviation()).isEqualTo("GO");
    }

    @Test
    void mapsAddressAndItsStateToResponse() {
        State state = new State("São Paulo", "SP");
        Address address = new Address(
                "Avenida Paulista",
                "Bela Vista",
                "Suite 1204",
                "B",
                "01310100",
                state);

        AddressResponse response = ResponseMapper.toResponse(address);

        assertThat(response.street()).isEqualTo("Avenida Paulista");
        assertThat(response.postalCode()).isEqualTo("01310100");
        assertThat(response.state().abbreviation()).isEqualTo("SP");
    }

    @Test
    void mapsPatientAndNestedRelationshipsToResponse() {
        State state = new State("Goiás", "GO");
        Address address = new Address(
                "Avenida T-10",
                "Setor Bueno",
                "Dental clinic",
                "A",
                "74223060",
                state);
        Patient patient = new Patient(
                "Ana Souza",
                LocalDate.of(1992, Month.APRIL, 18),
                true,
                Gender.FEMALE,
                "12345678901",
                address);

        PatientResponse response = ResponseMapper.toResponse(patient);

        assertThat(response.name()).isEqualTo("Ana Souza");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1992, Month.APRIL, 18));
        assertThat(response.active()).isTrue();
        assertThat(response.gender()).isEqualTo(Gender.FEMALE);
        assertThat(response.address().state().abbreviation()).isEqualTo("GO");
    }
}
