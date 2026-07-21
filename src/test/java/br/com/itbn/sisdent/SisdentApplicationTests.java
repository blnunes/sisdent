package br.com.itbn.sisdent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SisdentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadsStaticJsonDataAndReturnsPatients() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$[0].address.state.abbreviation").value("GO"));
    }

    @Test
    void returnsAddressByPostalCode() throws Exception {
        mockMvc.perform(get("/api/addresses/postal-code/01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.state.abbreviation").value("SP"));
    }

    @Test
    void returnsAllSeededStates() throws Exception {
        mockMvc.perform(get("/api/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].abbreviation").value("GO"))
                .andExpect(jsonPath("$[1].abbreviation").value("SP"));
    }

    @Test
    void returnsAllSeededAddresses() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void returnsPatientById() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.birthDate").value("1992-04-18"));
    }

    @Test
    void returnsNotFoundForUnknownPatient() throws Exception {
        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noLongerExposesMessageEndpoint() throws Exception {
        mockMvc.perform(get("/api/messages/hello"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsPatientWithAnExistingAddress() throws Exception {
        String request = """
                {
                  "name": "Maria Oliveira",
                  "birthDate": "1988-09-12",
                  "active": true,
                  "gender": "FEMALE",
                  "taxId": "98765432100",
                  "address": {
                    "street": "Avenida Paulista",
                    "district": "Bela Vista",
                    "additionalInfo": "Suite 1204",
                    "block": "A",
                    "postalCode": "01310100",
                    "state": {"name": "São Paulo", "abbreviation": "SP"}
                  }
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Oliveira"))
                .andExpect(jsonPath("$.address.postalCode").value("01310100"));
    }

    @Test
    void rejectsInvalidPatientRequest() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
