package br.com.itbn.sisdent;

import br.com.itbn.sisdent.config.InitialDataLoader;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.model.CatalogStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void grantsTheTrainingAdministratorAccessToEverySeededOrganization() {
        var administrator = accountRepository.findByEmail("admin@sisdent.local").orElseThrow();

        assertThat(membershipRepository.findAllByAccount_IdAndActiveTrue(administrator.getId()))
                .extracting(membership -> membership.getOrganization().getName())
                .contains("Northstar Dental Group", "Southstart Dental Group", "Harbor Dental Clinic");
    }

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sisdent API"))
                .andExpect(jsonPath("$.paths['/api/patients']").exists());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/specialities']").exists());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.identificationNumber.example")
                        .value("ADMIN"))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.example")
                        .value("admin"));
    }

    @Test
    void loadsStaticJsonDataAndReturnsPatients() throws Exception {
        mockMvc.perform(get("/api/patients").param("size", "100").param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(48))
                .andExpect(jsonPath("$.content[0].name").value("Abigail Scott"))
                .andExpect(jsonPath("$.content[0].address.administrativeDivision.code").value("IL"))
                .andExpect(jsonPath("$.content[0].globalId").isString())
                .andExpect(jsonPath("$.content[0].address.country.code").value("US"))
                .andExpect(jsonPath("$.content[0].nationality.code").value("US"))
                .andExpect(jsonPath("$.content[0].identificationType").value("NATIONAL_ID_CARD"))
                .andExpect(jsonPath("$.content[0].documentIssuerCountry.code").value("US"))
                .andExpect(jsonPath("$.content[0].identificationNumber").value("US10000000021"))
                .andExpect(jsonPath("$.content[0].specialities.length()").value(2))
                .andExpect(jsonPath("$.content[0].specialities[0].name").value("Endodontics"));
    }

    @Test
    void filtersPatientsOnTheServerBeforePaginating() throws Exception {
        mockMvc.perform(get("/api/patients")
                        .param("identificationNumber", "US10000000021")
                        .param("active", "true")
                        .param("gender", "FEMALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Abigail Scott"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].gender").value("FEMALE"));
    }

    @Test
    void suggestsAndFiltersPatientsByAssignedSpeciality() throws Exception {
        Long specialityId = specialityRepository.findByName("Endodontics").orElseThrow().getId();

        mockMvc.perform(get("/api/patients/filter-options")
                        .param("field", "specialityId")
                        .param("query", "endo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(specialityId.toString()))
                .andExpect(jsonPath("$[0].label").value("Endodontics"));

        mockMvc.perform(get("/api/patients").param("specialityId", specialityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].specialities[0].id").value(specialityId));
    }

    @Test
    void restoresMissingInitialDataWithoutDuplicatingExistingData() throws Exception {
        Patient patient = patientRepository.findByTaxId("10000000048").orElseThrow();
        patientRepository.delete(patient);
        patientRepository.flush();

        initialDataLoader.run(new DefaultApplicationArguments(new String[0]));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(48));
    }

    @Test
    void returnsAddressByPostalCode() throws Exception {
        mockMvc.perform(get("/api/addresses/postal-code/10000001").param("countryCode", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("152 Hudson Square Avenue"))
                .andExpect(jsonPath("$[0].administrativeDivision.code").value("NY"));
    }

    @Test
    void returnsAllSeededSpecialities() throws Exception {
        mockMvc.perform(get("/api/specialities").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.content[0].name").value("Dental Anesthesiology"))
                .andExpect(jsonPath("$.content[0].procedures.length()").value(2))
                .andExpect(jsonPath("$.content[0].procedures[0].name").value("Local anesthesia"))
                .andExpect(jsonPath("$.content[11].name").value("Prosthodontics"));
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
        var removedProcedure = speciality.getProcedures().stream()
                .filter(procedure -> !procedure.getId().equals(retainedProcedure.getId()))
                .findFirst()
                .orElseThrow();
        long originalVersion = speciality.getVersion();
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

        specialityRepository.flush();
        var updated = specialityRepository.findById(speciality.getId()).orElseThrow();
        assertThat(updated.findProcedure(removedProcedure.getId()).orElseThrow().getStatus())
                .isEqualTo(CatalogStatus.INACTIVE);
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
    }

    @Test
    void deactivatesSpecialityAndProceduresWithoutDeletingHistory() throws Exception {
        var speciality = specialityRepository.findByName("Orthodontics and Dentofacial Orthopedics")
                .orElseThrow();
        Long specialityId = speciality.getId();

        mockMvc.perform(delete("/api/specialities/{id}", specialityId))
                .andExpect(status().isNoContent());

        specialityRepository.flush();
        var deactivated = specialityRepository.findById(specialityId).orElseThrow();
        assertThat(deactivated.getStatus()).isEqualTo(CatalogStatus.INACTIVE);
        assertThat(deactivated.getProcedures())
                .allSatisfy(procedure -> assertThat(procedure.getStatus()).isEqualTo(CatalogStatus.INACTIVE));

        mockMvc.perform(get("/api/specialities").param("name", "Orthodontics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void doesNotExposeStandaloneProcedureEndpoint() throws Exception {
        mockMvc.perform(get("/api/procedures"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsAllSeededAdministrativeDivisions() throws Exception {
        mockMvc.perform(get("/api/administrative-divisions").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.content[0].code").value("CA"))
                .andExpect(jsonPath("$.content[0].country.code").value("US"))
                .andExpect(jsonPath("$.content[7].code").value("WA"));
    }

    @Test
    void returnsCountriesFromEuropeAndTheAmericas() throws Exception {
        mockMvc.perform(get("/api/countries").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(80))
                .andExpect(jsonPath("$.content[?(@.code == 'BR')].continent")
                        .value("SOUTH_AMERICA"))
                .andExpect(jsonPath("$.content[?(@.code == 'PT')].continent")
                        .value("EUROPE"))
                .andExpect(jsonPath("$.content[?(@.code == 'US')].continent")
                        .value("NORTH_AMERICA"));
    }

    @Test
    void returnsAllSeededAddresses() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(20));
    }

    @Test
    void returnsPatientById() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Olivia Bennett"))
                .andExpect(jsonPath("$.birthDate").value("1992-04-18"));
    }

    @Test
    void storesPatientAuditAndGlobalIdentityMetadata() {
        Patient patient = patientRepository.findById(1L).orElseThrow();

        assertThat(patient.getGlobalId()).isNotNull();
        assertThat(patient.getCreatedAt()).isNotNull();
        assertThat(patient.getUpdatedAt()).isNotNull();
        assertThat(patient.getCreatedBy()).isNotBlank();
        assertThat(patient.getUpdatedBy()).isNotBlank();
        assertThat(patient.getVersion()).isNotNegative();
    }

    @Test
    void returnsNotFoundForUnknownPatient() throws Exception {
        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesPatientByRemovingOnlyTheirSpecialityLinks() throws Exception {
        Patient patientBeforeUpdate = patientRepository.findById(1L).orElseThrow();
        var specialityIdsBeforeUpdate = patientBeforeUpdate.getSpecialities().stream()
                .map(speciality -> speciality.getId())
                .toList();

        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Olivia Bennett",
                                  "birthDate": "1992-04-18",
                                  "active": true,
                                  "gender": "FEMALE",
                                  "taxId": "10000000001",
                                  "identificationType": "NATIONAL_ID_CARD",
                                  "identificationNumber": "US10000000001",
                                  "documentIssuerCountryCode": "US",
                                  "nationalityCode": "US",
                                  "specialityIds": [],
                                  "address": {
                                    "street": "1842 Maple Grove Avenue",
                                    "district": "North Loop",
                                    "city": "Austin",
                                    "additionalInfo": "Apartment 3B",
                                    "block": "Building A",
                                    "postalCode": "73000001",
                                    "administrativeDivision": {"name": "Texas", "code": "TX", "type": "STATE"},
                                    "countryCode": "US"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.specialities.length()").value(0));

        patientRepository.flush();

        assertThat(patientRepository.findById(1L).orElseThrow().getSpecialities()).isEmpty();
        assertThat(specialityIdsBeforeUpdate)
                .allSatisfy(specialityId -> assertThat(specialityRepository.existsById(specialityId)).isTrue());
    }

    @Test
    void rejectsPatientResponseJsonWhenUsedAsUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1,
                                  "name": "Olivia Bennett",
                                  "birthDate": "2026-07-02",
                                  "active": true,
                                  "gender": "FEMALE",
                                  "taxId": "10000000001",
                                  "identificationType": "NATIONAL_ID_CARD",
                                  "identificationNumber": "US10000000001",
                                  "nationality": {"id": 1, "name": "United States", "code": "US", "continent": "NORTH_AMERICA"},
                                  "address": {
                                    "id": 1,
                                    "street": "1842 Maple Grove Avenue",
                                    "district": "North Loop",
                                    "city": "Austin",
                                    "additionalInfo": "Apartment 3B",
                                    "block": "Building A",
                                    "postalCode": "73000001",
                                    "administrativeDivision": {"id": 6, "name": "Texas", "code": "TX", "type": "STATE"},
                                    "country": {"id": 1, "name": "United States", "code": "US", "continent": "NORTH_AMERICA"}
                                  },
                                  "specialities": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresSpecialityIdsWhenUpdatingPatient() throws Exception {
        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Olivia Bennett",
                                  "birthDate": "1992-04-18",
                                  "active": true,
                                  "gender": "FEMALE",
                                  "taxId": "10000000001",
                                  "identificationType": "NATIONAL_ID_CARD",
                                  "identificationNumber": "US10000000001",
                                  "documentIssuerCountryCode": "US",
                                  "nationalityCode": "US",
                                  "address": {
                                    "street": "1842 Maple Grove Avenue",
                                    "district": "North Loop",
                                    "city": "Austin",
                                    "additionalInfo": "Apartment 3B",
                                    "block": "Building A",
                                    "postalCode": "73000001",
                                    "administrativeDivision": {"name": "Texas", "code": "TX", "type": "STATE"},
                                    "countryCode": "US"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noLongerExposesMessageEndpoint() throws Exception {
        mockMvc.perform(get("/api/messages/hello"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsPatientWithoutReusingAnExistingPostalCodeAddress() throws Exception {
        String request = """
                {
                  "name": "Maria Oliveira",
                  "birthDate": "1988-09-12",
                  "active": true,
                  "gender": "FEMALE",
                  "taxId": "98765432100",
                  "identificationType": "PASSPORT",
                  "identificationNumber": "BR 12-345 ABC",
                  "documentIssuerCountryCode": "BR",
                  "nationalityCode": "BR",
                  "specialityIds": [1, 3],
                  "address": {
                    "street": "152 Hudson Square Avenue",
                    "district": "Chelsea",
                    "city": "New York",
                    "additionalInfo": "Apartment 11D",
                    "block": "North Tower",
                    "postalCode": "10000001",
                    "administrativeDivision": {"name": "New York", "code": "NY", "type": "STATE"},
                    "countryCode": "US"
                  }
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Oliveira"))
                .andExpect(jsonPath("$.identificationType").value("PASSPORT"))
                .andExpect(jsonPath("$.identificationNumber").value("BR12345ABC"))
                .andExpect(jsonPath("$.nationality.code").value("BR"))
                .andExpect(jsonPath("$.address.postalCode").value("10000001"));
    }

    @Test
    void rejectsDuplicateNormalizedIdentificationNumberAtDatabaseBoundary() throws Exception {
        String firstRequest = patientRequest(
                "Unique One",
                "98765432101",
                "PT AB-12345");
        String duplicateRequest = patientRequest(
                "Unique Two",
                "98765432102",
                "ptab 12345");

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationNumber").value("PTAB12345"));

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Database constraint violation"));
    }

    @Test
    void rejectsInvalidPatientRequest() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String patientRequest(String name, String taxId, String identificationNumber) {
        return """
                {
                  "name": "%s",
                  "birthDate": "1988-09-12",
                  "active": true,
                  "gender": "FEMALE",
                  "taxId": "%s",
                  "identificationType": "NATIONAL_ID_CARD",
                  "identificationNumber": "%s",
                  "documentIssuerCountryCode": "PT",
                  "nationalityCode": "PT",
                  "specialityIds": [1],
                  "address": {
                    "street": "152 Hudson Square Avenue",
                    "district": "Chelsea",
                    "city": "New York",
                    "additionalInfo": "Apartment 11D",
                    "block": "North Tower",
                    "postalCode": "10000001",
                    "administrativeDivision": {"name": "New York", "code": "NY", "type": "STATE"},
                    "countryCode": "US"
                  }
                }
                """.formatted(name, taxId, identificationNumber);
    }
}
