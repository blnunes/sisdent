package br.com.itbn.sisdent;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Permission;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.UserRepository;
import br.com.itbn.sisdent.service.IdentificationNumbers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void requiresAuthenticationForBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsSwaggerEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }

    @Test
    void adminCanManageUsersAndPermissionsWithSoftDelete() throws Exception {
        String adminToken = emailLogin("admin@sisdent.local", "admin");
        String createRequest = """
                {
                  "identificationType": "PASSPORT",
                  "identificationNumber": "pT 123-aBc",
                  "password": "manager-password",
                  "role": "MANAGER"
                }
                """;

        String response = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationNumber").value("PT123ABC"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.permissions.length()").value(7))
                .andReturn().getResponse().getContentAsString();

        long userId = jsonMapper.readTree(response).get("id").asLong();
        mockMvc.perform(put("/api/users/{id}/permissions", userId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions": ["READ_PATIENTS", "READ_SPECIALITIES"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(2));

        mockMvc.perform(put("/api/users/{id}/permissions", userId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions": ["READ_PERMISSIONS", "MAINTAIN_PERMISSIONS"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(2));

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        User deleted = userRepository.findById(userId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.isActive()).isFalse();
        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerCanWriteBusinessDataButCannotAccessUsers() throws Exception {
        saveUser(IdentificationType.NATIONAL_ID, "MANAGER-001", "manager-password", Role.MANAGER);
        String token = login("NATIONAL_ID", "MANAGER-001", "manager-password");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/specialities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Manager speciality",
                                  "procedures": [{"name": "Manager procedure"}]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void regularUserCanOnlyReadBusinessDataAndCannotReadUsers() throws Exception {
        saveUser(IdentificationType.PASSPORT, "USER-001", "regular-password", Role.USER);
        String token = login("PASSPORT", "USER-001", "regular-password");

        mockMvc.perform(get("/api/patients")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/specialities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forbidden", "procedures": []}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void routeSpecificPermissionsRestrictAccessToConfiguredRoutes() throws Exception {
        userRepository.saveAndFlush(new User(
                IdentificationType.PASSPORT,
                IdentificationNumbers.normalize("ROUTE-USER"),
                passwordEncoder.encode("route-password"),
                Role.MANAGER,
                Set.of(Permission.READ_PATIENTS)));
        String token = login("PASSPORT", "ROUTE-USER", "route-password");

        mockMvc.perform(get("/api/patients")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/specialities")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void permissionsManagerCanUpdatePermissionsButCannotManageUsers() throws Exception {
        User permissionsManager = userRepository.saveAndFlush(new User(
                IdentificationType.PASSPORT,
                IdentificationNumbers.normalize("PERMISSIONS-MANAGER"),
                passwordEncoder.encode("permissions-password"),
                Role.MANAGER,
                Set.of(Permission.MAINTAIN_PERMISSIONS)));
        User target = userRepository.saveAndFlush(new User(
                IdentificationType.PASSPORT,
                IdentificationNumbers.normalize("PERMISSIONS-TARGET"),
                passwordEncoder.encode("target-password"),
                Role.USER,
                Set.of(Permission.READ_PATIENTS)));
        String token = login("PASSPORT", "PERMISSIONS-MANAGER", "permissions-password");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/users/{id}/permissions", target.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"READ_SPECIALITIES\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").value("READ_SPECIALITIES"));
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificationType\":\"PASSPORT\",\"identificationNumber\":\"BLOCKED\",\"password\":\"password-123\",\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificationType": "NATIONAL_ID",
                                  "identificationNumber": "ADMIN",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanChangeOwnPassword() throws Exception {
        saveUser(IdentificationType.PASSPORT, "PASSWORD-USER", "old-password", Role.USER);
        String token = login("PASSPORT", "PASSWORD-USER", "old-password");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "old-password",
                                  "newPassword": "new-secure-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificationType": "PASSPORT",
                                  "identificationNumber": "PASSWORD-USER",
                                  "password": "old-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
        login("PASSPORT", "PASSWORD-USER", "new-secure-password");
    }

    @Test
    void rejectsOwnPasswordChangeWhenCurrentPasswordIsWrong() throws Exception {
        saveUser(IdentificationType.NATIONAL_ID, "PASSWORD-FAIL", "correct-password", Role.MANAGER);
        String token = login("NATIONAL_ID", "PASSWORD-FAIL", "correct-password");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrong-password",
                                  "newPassword": "new-secure-password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Password change failed"))
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

        login("NATIONAL_ID", "PASSWORD-FAIL", "correct-password");
    }

    private void saveUser(
            IdentificationType type,
            String number,
            String password,
            Role role) {
        userRepository.saveAndFlush(new User(
                type,
                IdentificationNumbers.normalize(number),
                passwordEncoder.encode(password),
                role,
                role.defaultPermissions()));
    }

    private String login(String type, String number, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificationType": "%s",
                                  "identificationNumber": "%s",
                                  "password": "%s"
                                }
                                """.formatted(type, number, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = jsonMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private String emailLogin(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
