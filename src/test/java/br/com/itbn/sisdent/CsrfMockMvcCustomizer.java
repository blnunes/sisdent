package br.com.itbn.sisdent;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Supplies valid CSRF tokens to existing MockMvc requests; production clients obtain theirs from /api/csrf. */
@Component
class CsrfMockMvcCustomizer implements MockMvcBuilderCustomizer {

    @Override
    public void customize(org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(MockMvcRequestBuilders.get("/").with(csrf()));
    }
}
