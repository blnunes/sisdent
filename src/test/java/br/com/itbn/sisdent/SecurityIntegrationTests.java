package br.com.itbn.sisdent;

import java.util.List;
import jakarta.servlet.http.Cookie;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
    void providesAnAntiCsrfTokenForTheSinglePageApplication() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty());
    }

    @Test
    void rejectsUnsafeRequestsThatCarryASessionCookieWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("JSESSIONID", "browser-session"))
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@sisdent.local\",\"password\":\"admin\"}"))
                .andExpect(status().isForbidden());
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
    void platformAdministratorCanUsePlatformTranslationCatalogueThroughGraphQl() throws Exception {
        String token = emailLogin("admin@sisdent.local", "admin");

        mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ catalogTranslations { canonicalName } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void organizationAdministratorCannotManagePlatformTranslationsThroughGraphQl() throws Exception {
        String token = emailLogin("group.admin@sisdent.demo", "odonto2026@O");

        mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"query\": \"{ catalogTranslations { canonicalName } }\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    @Test
    void anyAuthenticatedUserCanChangeOnlyTheirOwnSettingsAndPassword() throws Exception {
        String oldPassword = "odonto2026@O";
        String newPassword = "changed-password-2026";
        String token = emailLogin("northstar.readonly@sisdent.demo", oldPassword);
        String settings = mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ currentAccountSettings { email version } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAccountSettings.email").value("northstar.readonly@sisdent.demo"))
                .andReturn().getResponse().getContentAsString();
        long version = jsonMapper.readTree(settings).get("data").get("currentAccountSettings").get("version").asLong();

        mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { updateOwnProfile(input: { displayName: \\\"Own settings user\\\", version: %d }) { displayName } }\"}".formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateOwnProfile.displayName").value("Own settings user"));
        mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { changeOwnPassword(input: { currentPassword: \\\"%s\\\", newPassword: \\\"%s\\\" }) }\"}".formatted(oldPassword, newPassword)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.changeOwnPassword").value(true));

        emailLogin("northstar.readonly@sisdent.demo", newPassword);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"northstar.readonly@sisdent.demo\",\"password\":\"%s\"}".formatted(oldPassword)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ currentAccountSettings { email } }\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserPersistsOwnPreferredLanguageAcrossSessions() throws Exception {
        String token = emailLogin("northstar.readonly@sisdent.demo", "odonto2026@O");

        mockMvc.perform(post("/graphql").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"mutation { updateOwnPreferredLanguage(input: { preferredLanguage: \\\"nl\\\" }) { preferredLanguage } }\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.updateOwnPreferredLanguage.preferredLanguage").value("nl"));
        mockMvc.perform(get("/api/session").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferredLanguage").value("nl"));
        String renewedToken = emailLogin("northstar.readonly@sisdent.demo", "odonto2026@O");
        mockMvc.perform(get("/api/session").header("Authorization", bearer(renewedToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferredLanguage").value("nl"));
        mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { updateOwnPreferredLanguage(input: { preferredLanguage: \\\"pt-BR\\\" }) { preferredLanguage } }\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retiredSpecialityRestOperationsReturnNotFound() throws Exception {
        String token = emailLogin("admin@sisdent.local", "admin");

        mockMvc.perform(get("/api/specialities")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/specialities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/specialities/{id}", 1)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
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
