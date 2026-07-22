package br.com.itbn.sisdent;

import org.junit.jupiter.api.Tag;
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
@Tag("integration")
class SisdentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sisdent API"))
                .andExpect(jsonPath("$.paths['/api/patients']").exists());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/specialities']").exists());
    }

    @Test
    void loadsStaticJsonDataAndReturnsPatients() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$[0].address.state.abbreviation").value("GO"))
                .andExpect(jsonPath("$[0].specialities.length()").value(2))
                .andExpect(jsonPath("$[0].specialities[0].name").value("Ortodontia"));
    }

    @Test
    void returnsAddressByPostalCode() throws Exception {
        mockMvc.perform(get("/api/addresses/postal-code/01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.state.abbreviation").value("SP"));
    }

    @Test
    void returnsAllSeededSpecialities() throws Exception {
        mockMvc.perform(get("/api/specialities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Endontia"))
                .andExpect(jsonPath("$[1].name").value("Ortodontia"))
                .andExpect(jsonPath("$[2].name").value("Pediatric"));
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
                  "specialityIds": [1, 3],
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
