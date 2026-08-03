package br.com.itbn.sisdent;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class SecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private JwtDecoder jwtDecoder;

    @Test
    void requiresAuthenticationForBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noLongerExposesTheLegacyUsersApi() throws Exception {
        String token = emailLogin("admin@sisdent.local", "admin");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenContainsOnlyThePlatformAuthorityNotLegacyRolesOrPermissions() throws Exception {
        Jwt jwt = jwtDecoder.decode(emailLogin("admin@sisdent.local", "admin"));

        List<String> authorities = jwt.getClaimAsStringList("authorities");
        assertThat(authorities).containsExactly("ROLE_PLATFORM_ADMIN");
        assertThat(jwt.getClaims()).doesNotContainKeys("userId");
    }

    @Test
    void platformAdministratorCanUsePlatformCatalogues() throws Exception {
        String token = emailLogin("admin@sisdent.local", "admin");

        mockMvc.perform(get("/api/specialities").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private String emailLogin(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
