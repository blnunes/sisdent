package br.com.itbn.sisdent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AccountAvatarIntegrationTests {
    private static final Path AVATARS = createAvatarDirectory();
    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;

    @DynamicPropertySource
    static void avatarDirectory(DynamicPropertyRegistry registry) {
        registry.add("sisdent.avatar.storage-directory", AVATARS::toString);
    }

    @Test
    void uploadsAndDownloadsNormalizedAvatarThroughGraphQlWithoutRestRoutes() throws Exception {
        String token = login();
        upload(token, image()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadOwnAvatar.avatarUrl").value(org.hamcrest.Matchers.containsString("graphql-avatar")));
        mockMvc.perform(post("/graphql").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ ownAvatar { contentType contentBase64 } }\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.ownAvatar.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.ownAvatar.contentBase64").isNotEmpty());
        mockMvc.perform(post("/api/account/settings/avatar").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidAvatarAndUnauthenticatedGraphQlUpload() throws Exception {
        upload(login(), new MockMultipartFile("file", "invalid.png", "image/png", "not an image".getBytes()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.errors[0]").exists());
        upload(null, image()).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions upload(String token, MockMultipartFile file) throws Exception {
        String encoded = java.util.Base64.getEncoder().encodeToString(file.getBytes());
        var request = post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"mutation UploadOwnAvatar($input: AvatarUploadInput!) { uploadOwnAvatar(input: $input) { avatarUrl } }\",\"variables\":{\"input\":{\"fileName\":\"%s\",\"contentType\":\"%s\",\"contentBase64\":\"%s\"}}}".formatted(file.getOriginalFilename(), file.getContentType(), encoded));
        if (token != null) request.header("Authorization", bearer(token));
        return mockMvc.perform(request);
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"northstar.readonly@sisdent.demo\",\"password\":\"odonto2026@O\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private static MockMultipartFile image() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("0", "avatar.png", "image/png", output.toByteArray());
    }

    private static String bearer(String token) { return "Bearer " + token; }

    private static Path createAvatarDirectory() {
        try { return Files.createTempDirectory("sisdent-avatar-integration-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
}
