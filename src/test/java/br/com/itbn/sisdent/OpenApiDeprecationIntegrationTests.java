package br.com.itbn.sisdent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDeprecationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotPublishRetiredSpecialityOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/specialities'].get").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/specialities'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/specialities/{id}'].put").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/organizations/{organizationId}/patients/{patientId}'].put")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/countries'].get").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/countries'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/countries/{id}'].put").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/countries/continents'].get.deprecated").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/countries/{id}'].delete.deprecated").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/specialities/filter-options'].get").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/specialities/{id}'].delete").doesNotExist());
    }
}
