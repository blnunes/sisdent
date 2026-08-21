package br.com.itbn.sisdent;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import java.util.stream.Stream;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
@ActiveProfiles("test")
class GraphQlIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ClinicUnitRepository clinicUnitRepository;

    @Test
    void platformAdministratorCanQueryLocalizedCountries() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "{ countries(page: { page: 0, size: 1, sort: \\"name\\", direction: ASC }, locale: \\"pt-PT\\") { content { code name displayName continent } page size totalElements totalPages } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.countries.page").value(0))
                .andExpect(jsonPath("$.data.countries.size").value(1))
                .andExpect(jsonPath("$.data.countries.content[0].code").isNotEmpty())
                .andExpect(jsonPath("$.data.countries.content[0].displayName").isNotEmpty());
    }

    @Test
    void countryAuxiliaryOperationsUseGraphQlInsteadOfRest() throws Exception {
        String authorization = bearer(emailLogin("admin@sisdent.local", "admin"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ continents }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.continents[0]").isNotEmpty());

        mockMvc.perform(post("/graphql")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"mutation { deleteCountry(id: \\\"1\\\") }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.deleteCountry").value(true));
    }

    @Test
    void graphqlRequiresPlatformAdministratorAuthority() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries { page } }\" }"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("group.admin@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countries").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void countryMutationUsesTheExistingServiceWorkflowAndSafeErrors() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .header("X-Correlation-ID", "country-mutation-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createCountry(input: { name: \\"GraphQL Isles\\", code: \\"GI\\", continent: EUROPE }) { id name code continent } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.createCountry.name").value("GraphQL Isles"))
                .andExpect(jsonPath("$.data.createCountry.code").value("GI"))
                .andExpect(header().string("X-Correlation-ID", "country-mutation-42"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { updateCountry(id: \\"1\\", input: { name: \\"Updated GraphQL Country\\", code: \\"UG\\", continent: EUROPE }) { id name code continent } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.updateCountry.name").value("Updated GraphQL Country"))
                .andExpect(jsonPath("$.data.updateCountry.code").value("UG"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createCountry(input: { name: \\"Invalid\\", code: \\"invalid\\", continent: EUROPE }) { id } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createCountry").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("VALIDATION.FAILED"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("group.admin@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { updateCountry(id: \\"1\\", input: { name: \\"Forbidden\\", code: \\"FB\\", continent: EUROPE }) { id } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateCountry").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void specialityMutationsReplaceTheRetiredRestOperations() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createSpeciality(input: { name: \\"GraphQL Implantology\\", procedures: [{ name: \\"Guided implant placement\\" }] }, locale: \\"pt-PT\\") { id name displayName procedures { name } } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.createSpeciality.name").value("GraphQL Implantology"))
                .andExpect(jsonPath("$.data.createSpeciality.procedures[0].name").value("Guided implant placement"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createSpeciality(input: { name: \\"\\", procedures: [] }) { id } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createSpeciality").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("VALIDATION.FAILED"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("group.admin@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { updateSpeciality(id: \\"1\\", input: { name: \\"Forbidden speciality\\", procedures: [{ name: \\"Forbidden procedure\\" }] }) { id } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateSpeciality").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void organizationMutationPreservesAuthorizationAndTenantIsolation() throws Exception {
        String northstarId = organizationId("Northstar Dental Group");
        String outsideScopeId = "00000000-0000-0000-0000-000000000001";
        String token = bearer(emailLogin("group.admin@sisdent.demo", "odonto2026@O"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createClinicUnit(organizationId: \\"%s\\", input: { name: \\"GraphQL unit\\" }) { organizationId name active } }" }
                                """.formatted(northstarId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.createClinicUnit.organizationId").value(northstarId))
                .andExpect(jsonPath("$.data.createClinicUnit.name").value("GraphQL unit"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "mutation { createClinicUnit(organizationId: \\"%s\\", input: { name: \\"Outside scope\\" }) { id } }" }
                                """.formatted(outsideScopeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createClinicUnit").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void patientUpdateMutationUsesTheScopedServiceWorkflowAndSafeErrors() throws Exception {
        String organizationId = organizationId("Northstar Dental Group");
        String authorization = bearer(emailLogin("group.admin@sisdent.demo", "odonto2026@O"));
        String patientId = createPatientForGraphQlUpdate(organizationId, authorization);

        mockMvc.perform(post("/graphql")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientUpdateMutation(organizationId, patientId, "GraphQL Updated Patient")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.updatePatient.globalId").value(patientId))
                .andExpect(jsonPath("$.data.updatePatient.name").value("GraphQL Updated Patient"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientUpdateMutation(organizationId, patientId, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatePatient").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("VALIDATION.FAILED"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("northstar.readonly@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientUpdateMutation(organizationId, patientId, "Forbidden Update")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatePatient").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @ParameterizedTest
    @MethodSource("invalidPaginationQueries")
    void invalidPaginationIsReportedAsAGraphQlError(
            String query,
            String expectedCode,
            String expectedMessage) throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"%s\" }".formatted(query.replace("\"", "\\\""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countries").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value(expectedMessage))
                .andExpect(jsonPath("$.errors[0].extensions.code").value(expectedCode));
    }

    private static Stream<Arguments> invalidPaginationQueries() {
        return Stream.of(
                Arguments.of("{ countries(page: { page: -1 }) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(page: { size: 0 }) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(page: { size: 101 }) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(page: { sort: \"unknown\" }) { page } }", "PAGINATION.UNSUPPORTED_SORT", "The requested sort field is not supported."));
    }

    @Test
    void unsupportedLocaleIsReportedWithAnExplanation() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries(locale: \\\"zh-CN\\\") { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countries").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value(
                        "The requested catalogue locale is not supported. Supported locales are: en, nl, pt."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CATALOG.UNSUPPORTED_LOCALE"))
                .andExpect(jsonPath("$.errors[0].extensions.metadata.supportedLocales").value("en, nl, pt"));
    }

    @Test
    void missingCountryUsesTheSharedNotFoundErrorContract() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .header("X-Correlation-ID", "graphql-error-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ country(code: \\\"ZZ\\\") { code } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.country").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value("The requested country is not available."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CATALOG.UNKNOWN_COUNTRY"))
                .andExpect(jsonPath("$.errors[0].extensions.correlationId").value("graphql-error-42"))
                .andExpect(header().string("X-Correlation-ID", "graphql-error-42"));
    }

    @Test
    void malformedGraphQlDocumentUsesASafeLocalizedError() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries( { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value(
                        "The request body is malformed or cannot be read."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("REQUEST.MALFORMED"));
    }

    @Test
    void invalidGraphQlArgumentUsesASafeValidationError() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries(page: { page: \\\"zero\\\" }) { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value("One or more fields are invalid."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("VALIDATION.FAILED"));
    }

    @Test
    void paginationErrorUsesTheRequestLocaleForItsFriendlyMessage() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .header("Accept-Language", "pt-PT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries(page: { size: 101 }) { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value("Os valores de paginação são inválidos."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("PAGINATION.INVALID_VALUES"));
    }

    @Test
    void platformAdministratorCanQueryLocalizedFilteredSpecialities() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .header("X-Correlation-ID", "specialities-graphql-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ specialities(page: { page: 0, size: 10, sort: \\\"name\\\", direction: ASC }, filter: { name: \\\"a\\\" }, locale: \\\"nl-BE\\\") { content { id displayName procedures { displayName } } page totalElements } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.specialities.page").value(0))
                .andExpect(jsonPath("$.data.specialities.content").isArray())
                .andExpect(header().string("X-Correlation-ID", "specialities-graphql-42"));
    }

    @Test
    void organizationReadsPreserveServiceAuthorizationAndTenantIsolation() throws Exception {
        String northstarId = organizationId("Northstar Dental Group");
        String harborId = organizationId("Harbor Dental Clinic");

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("northstar.practitioners@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ practitioners(organizationId: \\\"%s\\\") { globalId displayName specialityIds } }\" }".formatted(northstarId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.practitioners[0].displayName").isNotEmpty())
                .andExpect(jsonPath("$.data.practitioners[0].registrationNumber").doesNotExist());

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("northstar.practitioners@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ practitioners(organizationId: \\\"%s\\\") { displayName } }\" }".formatted(harborId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.practitioners").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void clinicUnitQueryUsesOrganizationAndOptionalClinicScope() throws Exception {
        String northstarId = organizationId("Northstar Dental Group");
        String clinicUnitId = clinicUnitRepository
                .findAllByOrganization_GlobalIdAndActiveTrueOrderByName(java.util.UUID.fromString(northstarId))
                .getFirst().getGlobalId().toString();
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("northstar.scheduler@sisdent.demo", "odonto2026@O")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ clinicUnits(organizationId: \\\"%s\\\", clinicUnitId: \\\"%s\\\") { id organizationId name active } }\" }".formatted(northstarId, clinicUnitId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.clinicUnits[0].organizationId").value(northstarId))
                .andExpect(jsonPath("$.data.clinicUnits[0].id").value(clinicUnitId));
    }

    private String organizationId(String name) {
        return organizationRepository.findByName(name).orElseThrow().getGlobalId().toString();
    }

    private String createPatientForGraphQlUpdate(String organizationId, String authorization) throws Exception {
        String response = mockMvc.perform(post("/api/organizations/{organizationId}/patients", organizationId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "GraphQL Update Candidate",
                                  "birthDate": "1990-01-02",
                                  "active": true,
                                  "gender": "FEMALE",
                                  "taxId": null,
                                  "identificationType": "PASSPORT",
                                  "identificationNumber": "GQL-UPDATE-42",
                                  "documentIssuerCountryCode": "PT",
                                  "nationalityCode": "PT",
                                  "addressId": 1,
                                  "specialityIds": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(response).get("globalId").asString();
    }

    private static String patientUpdateMutation(String organizationId, String patientId, String name) {
        return """
                { "query": "mutation { updatePatient(organizationId: \\"%s\\", patientId: \\"%s\\", input: { name: \\"%s\\", birthDate: \\"1990-01-02\\", active: true, gender: FEMALE, taxId: null, identificationType: PASSPORT, identificationNumber: \\"GQL-UPDATE-42\\", documentIssuerCountryCode: \\"PT\\", nationalityCode: \\"PT\\", addressId: \\"1\\", specialityIds: [] }) { globalId name active } }" }
                """.formatted(organizationId, patientId, name);
    }

    private String emailLogin(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
