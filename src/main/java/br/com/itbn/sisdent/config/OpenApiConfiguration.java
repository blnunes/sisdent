package br.com.itbn.sisdent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI sisdentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sisdent API")
                        .version("v1")
                        .description("API REST para gerenciamento de pacientes, endereços e estados."));
    }
}
