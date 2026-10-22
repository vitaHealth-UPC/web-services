package com.vitahealth.tata.shared.infrastructure.documentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vitahealth.tata.shared.infrastructure.documentation.openapi.configuration.OpenApiConfiguration;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

/** Verifies deployment metadata and token documentation without assuming a JWT token format. */
class OpenApiConfigurationTest {
  @Test
  void usesConfiguredVersionAndBearerScheme() {
    var api = new OpenApiConfiguration().tataOpenApi("1.0.0");
    assertEquals("1.0.0", api.getInfo().getVersion());
    var bearer = api.getComponents().getSecuritySchemes().get("bearerAuth");
    assertEquals(SecurityScheme.Type.HTTP, bearer.getType());
    assertEquals("bearer", bearer.getScheme());
    assertNull(bearer.getBearerFormat());
  }
}
