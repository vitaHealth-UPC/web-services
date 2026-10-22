package com.vitahealth.tata.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies API metadata and the Bearer authentication scheme used by protected operations. */
@Configuration
public class OpenApiConfiguration {
  /** Builds documentation metadata from deployment configuration. */
  @Bean
  public OpenAPI tataOpenApi(@Value("${tata.api.version:1.0.0}") String version) {
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description(
                            "Session access token returned by the authentication endpoints")))
        .info(
            new Info()
                .title("Tata Web Services API")
                .version(version)
                .description("RESTful API for Tata medication adherence and family monitoring."));
  }
}
