package br.com.itbn.sisdent;

import br.com.itbn.sisdent.config.InitialDataLoader;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class SisdentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InitialDataLoader initialDataLoader;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SpecialityRepository specialityRepository;

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
                .andExpect(jsonPath("$.length()").value(40))
                .andExpect(jsonPath("$[0].name").value("Abigail Scott"))
                .andExpect(jsonPath("$[0].address.state.abbreviation").value("IL"))
                .andExpect(jsonPath("$[0].specialities.length()").value(2))
                .andExpect(jsonPath("$[0].specialities[0].name").value("Endodontics"));
    }

    @Test
    void restoresMissingInitialDataWithoutDuplicatingExistingData() throws Exception {
        Patient patient = patientRepository.findByTaxId("10000000001").orElseThrow();
        patientRepository.delete(patient);
        patientRepository.flush();

        initialDataLoader.run(new DefaultApplicationArguments(new String[0]));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(40));
    }

    @Test
    void returnsAddressByPostalCode() throws Exception {
        mockMvc.perform(get("/api/addresses/postal-code/10000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("152 Hudson Square Avenue"))
                .andExpect(jsonPath("$.state.abbreviation").value("NY"));
    }

    @Test
    void returnsAllSeededSpecialities() throws Exception {
        mockMvc.perform(get("/api/specialities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].name").value("Dental Anesthesiology"))
                .andExpect(jsonPath("$[0].procedures.length()").value(2))
                .andExpect(jsonPath("$[0].procedures[0].name").value("Local anesthesia"))
                .andExpect(jsonPath("$[11].name").value("Prosthodontics"));
    }

    @Test
    void createsSpecialityWithNestedProcedures() throws Exception {
        String request = """
                {
                  "name": "Implant Dentistry",
                  "procedures": [
                    {"name": "Implant placement"},
                    {"name": "Bone graft"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/specialities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Implant Dentistry"))
                .andExpect(jsonPath("$.procedures.length()").value(2))
                .andExpect(jsonPath("$.procedures[0].name").value("Bone graft"))
                .andExpect(jsonPath("$.procedures[1].name").value("Implant placement"));
    }

    @Test
    void updatesSpecialityAndItsNestedProcedures() throws Exception {
        var speciality = specialityRepository.findByName("Endodontics").orElseThrow();
        var retainedProcedure = speciality.getProcedures().stream()
                .filter(procedure -> procedure.getName().equals("Pulpotomy"))
                .findFirst()
                .orElseThrow();
        String request = """
                {
                  "name": "Advanced Endodontics",
                  "procedures": [
                    {"id": %d, "name": "Advanced pulpotomy"},
                    {"name": "Apicoectomy"}
                  ]
                }
                """.formatted(retainedProcedure.getId());

        mockMvc.perform(put("/api/specialities/{id}", speciality.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Advanced Endodontics"))
                .andExpect(jsonPath("$.procedures.length()").value(2))
                .andExpect(jsonPath("$.procedures[0].name").value("Advanced pulpotomy"))
                .andExpect(jsonPath("$.procedures[1].name").value("Apicoectomy"))
                .andExpect(jsonPath("$.procedures[1].id").isNumber());
    }

    @Test
    void doesNotExposeStandaloneProcedureEndpoint() throws Exception {
        mockMvc.perform(get("/api/procedures"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsAllSeededStates() throws Exception {
        mockMvc.perform(get("/api/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].abbreviation").value("CA"))
                .andExpect(jsonPath("$[7].abbreviation").value("WA"));
    }

    @Test
    void returnsAllSeededAddresses() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void returnsPatientById() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Olivia Bennett"))
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
                    "street": "152 Hudson Square Avenue",
                    "district": "Chelsea",
                    "additionalInfo": "Apartment 11D",
                    "block": "North Tower",
                    "postalCode": "10000001",
                    "state": {"name": "New York", "abbreviation": "NY"}
                  }
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Oliveira"))
                .andExpect(jsonPath("$.address.postalCode").value("10000001"));
    }

    @Test
    void rejectsInvalidPatientRequest() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
