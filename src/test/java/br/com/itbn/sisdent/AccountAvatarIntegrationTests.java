package br.com.itbn.sisdent;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AccountAvatarIntegrationTests {
    private static final Path AVATARS = createAvatarDirectory();
    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;

    @DynamicPropertySource
    static void avatarDirectory(DynamicPropertyRegistry registry) { registry.add("sisdent.avatar.storage-directory", () -> AVATARS.toString()); }

    @Test
    void uploadsPngAndJpegThroughMultipartThenReturnsNormalizedPng() throws Exception {
        String token = login();
        upload(token, image("image/png", "avatar.png", "png"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").exists());
        mockMvc.perform(get("/api/account/settings/avatar").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Length"));

        upload(token, image("image/jpeg", "avatar.jpeg", "jpeg")).andExpect(status().isOk());
    }

    @Test
    void returnsCorrelatedProblemDetailsForInvalidOrUnauthenticatedMultipartRequests() throws Exception {
        upload(login(), new MockMultipartFile("file", "invalid.png", "image/png", "not an image".getBytes()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ACCOUNT.AVATAR_INVALID_TYPE"));
        mockMvc.perform(multipart("/api/account/settings/avatar").file(image("image/png", "avatar.png", "png"))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION.FAILED"));
        mockMvc.perform(multipart("/api/account/settings/avatar")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("Authorization", bearer(login())).header("X-Correlation-ID", "avatar-invalid-42"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST.PARAMETER_INVALID"))
                .andExpect(header().string("X-Correlation-ID", "avatar-invalid-42"));
    }

    private org.springframework.test.web.servlet.ResultActions upload(String token, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/account/settings/avatar").file(file)
                .with(request -> { request.setMethod("PUT"); return request; }).header("Authorization", bearer(token)));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"northstar.readonly@sisdent.demo\",\"password\":\"odonto2026@O\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private static MockMultipartFile image(String contentType, String name, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), format, output);
        return new MockMultipartFile("file", name, contentType, output.toByteArray());
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private static Path createAvatarDirectory() {
        try { return Files.createTempDirectory("sisdent-avatar-integration-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
}
