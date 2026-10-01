package com.tata.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI tataOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Tata Web Services API")
                .description("RESTful API of Tata, built with Domain-Driven Design")
                .version("v1"));
    }
}
