package br.com.itbn.sisdent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
@ActiveProfiles("test")
class RestErrorIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @ParameterizedTest
    @MethodSource("invalidPagination")
    void restPaginationUsesTheSharedApplicationErrorContract(String parameter, String value, String code, String message)
            throws Exception {
        mockMvc.perform(get("/api/countries").param(parameter, value).header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:sisdent:error:" + code.toLowerCase()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.code").value(code));
    }

    private static Stream<Arguments> invalidPagination() {
        return Stream.of(
                Arguments.of("page", "-1", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("size", "101", "PAGINATION.INVALID_VALUES", "Pagination values are invalid."),
                Arguments.of("sort", "unknown", "PAGINATION.UNSUPPORTED_SORT", "The requested sort field is not supported."),
                Arguments.of("direction", "sideways", "PAGINATION.UNSUPPORTED_DIRECTION", "The requested sort direction is not supported."));
    }

    @Test
    void restPaginationLocalizesTheFriendlyMessage() throws Exception {
        mockMvc.perform(get("/api/countries").param("size", "101").header("Accept-Language", "pt-PT")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Os valores de paginação são inválidos."))
                .andExpect(jsonPath("$.code").value("PAGINATION.INVALID_VALUES"));
    }

    @Test
    void beanValidationProvidesSafeFieldViolations() throws Exception {
        mockMvc.perform(post("/api/countries").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"code\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION.FAILED"))
                .andExpect(jsonPath("$.detail").value("One or more fields are invalid."))
                .andExpect(jsonPath("$.violations[0].field").value("code"))
                .andExpect(jsonPath("$.violations[0].message").value("must match \"[A-Z][A-Z]{2}\""))
                .andExpect(jsonPath("$.violations[1].field").value("continent"))
                .andExpect(jsonPath("$.violations[1].message").value("must not be null"))
                .andExpect(jsonPath("$.violations[2].field").value("name"))
                .andExpect(jsonPath("$.violations[2].message").value("must not be blank"));
    }

    @Test
    void malformedAndInvalidParametersUseSafeRequestErrors() throws Exception {
        mockMvc.perform(post("/api/countries").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST.MALFORMED"))
                .andExpect(jsonPath("$.detail").value("The request body is malformed or cannot be read."));

        mockMvc.perform(get("/api/countries").param("page", "invalid").header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST.PARAMETER_INVALID"))
                .andExpect(jsonPath("$.detail").value("A request parameter is invalid or missing."));
    }

    @Test
    void unauthenticatedAndUnauthorizedRequestsUseTheProblemContract() throws Exception {
        mockMvc.perform(get("/api/countries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION.FAILED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."));

        mockMvc.perform(get("/api/countries").header("Authorization", bearer(login("group.admin@sisdent.demo", "odonto2026@O"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION.DENIED"))
                .andExpect(jsonPath("$.detail").value("You are not allowed to access this resource."));
    }

    private String adminToken() throws Exception {
        return login("admin@sisdent.local", "admin");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
