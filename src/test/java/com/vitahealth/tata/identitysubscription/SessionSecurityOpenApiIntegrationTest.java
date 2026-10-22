package com.vitahealth.tata.identitysubscription;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Verifies published Swagger evidence matches actual public and authenticated API operations. */
@SpringBootTest
@ActiveProfiles("test")
class SessionSecurityOpenApiIntegrationTest {
  @Autowired private WebApplicationContext context;

  @Test
  void publicDocumentDistinguishesRegistrationFromPrivateSessionAndSubscriptionOperations()
      throws Exception {
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/api/v1/accounts'].post.security").isEmpty())
        .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.security").isEmpty())
        .andExpect(jsonPath("$.paths['/api/v1/plans'].get.security").isEmpty())
        .andExpect(
            jsonPath("$.paths['/api/v1/sessions/current'].get.security[0].bearerAuth").isArray());
  }
}
