package br.com.itbn.sisdent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.itbn.sisdent.error.BusinessRuleViolationException;
import br.com.itbn.sisdent.error.ConflictException;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.CountryService;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@ActiveProfiles("test")
class GraphQlErrorExecutionIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private CountryService countryService;

    @ParameterizedTest
    @MethodSource("executionFailures")
    void executionFailuresUseExactSafeGraphQlContract(
            RuntimeException exception, String expectedCode, String expectedMessage) throws Exception {
        when(countryService.findPage(any(PageQuery.class), any(Locale.class))).thenThrow(exception);

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ countries { page } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countries").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value(expectedMessage))
                .andExpect(jsonPath("$.errors[0].extensions.code").value(expectedCode));
    }

    static Stream<Arguments> executionFailures() {
        return Stream.of(
                Arguments.of(new ConflictException(ErrorCode.CONFLICT),
                        "CONFLICT", "The request conflicts with the current state of the resource."),
                Arguments.of(new BusinessRuleViolationException(ErrorCode.BUSINESS_RULE_VIOLATION),
                        "BUSINESS_RULE.VIOLATION", "The request violates a business rule."),
                Arguments.of(new IllegalStateException("database password and clinical note"),
                        "INTERNAL.ERROR", "An unexpected error occurred. Please try again later."));
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@sisdent.local\",\"password\":\"admin\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
