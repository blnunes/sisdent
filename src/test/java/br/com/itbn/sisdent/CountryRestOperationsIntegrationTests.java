package br.com.itbn.sisdent;

import br.com.itbn.sisdent.repository.CountryRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
@ActiveProfiles("test")
class CountryRestOperationsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CountryRepository countries;

    @Test
    void retiredCountryListCreateAndUpdateRoutesHaveNoMapping() throws Exception {
        String authorization = bearer(adminToken());

        mockMvc.perform(get("/api/countries").header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/countries")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/countries/{id}", 1L)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retainedContinentsAndDeleteRoutesRemainAvailable() throws Exception {
        String authorization = bearer(adminToken());
        Long countryId = countries.findAll().getFirst().getId();

        mockMvc.perform(get("/api/countries/continents").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNotEmpty());
        mockMvc.perform(delete("/api/countries/{id}", countryId).header("Authorization", authorization))
                .andExpect(status().isNoContent());
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@sisdent.local\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
