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
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;
import java.util.stream.Stream;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
@ActiveProfiles("test")
class GraphQlIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @Test
    void platformAdministratorCanQueryLocalizedCountries() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "query": "{ countries(page: 0, size: 1, sort: \\"name\\", locale: \\"pt-PT\\") { content { code name displayName continent } page size totalElements totalPages } }" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.countries.page").value(0))
                .andExpect(jsonPath("$.data.countries.size").value(1))
                .andExpect(jsonPath("$.data.countries.content[0].code").isNotEmpty())
                .andExpect(jsonPath("$.data.countries.content[0].displayName").isNotEmpty());
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
                .andExpect(status().isForbidden());
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
                Arguments.of("{ countries(page: -1) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(size: 0) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(size: 101) { page } }", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("{ countries(sort: \"unknown\") { page } }", "PAGINATION.UNSUPPORTED_SORT", "The requested sort field is not supported."),
                Arguments.of("{ countries(direction: \"sideways\") { page } }", "PAGINATION.UNSUPPORTED_DIRECTION", "The requested sort direction is not supported."));
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
                        "The requested locale \"zh-CN\" is not supported. Supported locales are: en, nl, pt."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CATALOG.UNSUPPORTED_LOCALE"));
    }

    @Test
    void missingCountryUsesTheSharedNotFoundErrorContract() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(emailLogin("admin@sisdent.local", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ country(code: \\\"ZZ\\\") { code } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.country").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value("The requested country is not available."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CATALOG.UNKNOWN_COUNTRY"));
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
                        .content("{ \"query\": \"{ countries(page: \\\"zero\\\") { page } }\" }"))
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
                        .content("{ \"query\": \"{ countries(size: 101) { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value("Os valores de paginação são inválidos."))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("PAGINATION.INVALID_VALUES"));
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
