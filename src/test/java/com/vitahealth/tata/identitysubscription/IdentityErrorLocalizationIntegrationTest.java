package com.vitahealth.tata.identitysubscription;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies localization for explicit failures and validation exceptions at the real HTTP boundary.
 */
@SpringBootTest
@ActiveProfiles("test")
class IdentityErrorLocalizationIntegrationTest {
  @Autowired private WebApplicationContext context;

  @Test
  void regionalAndGenericSpanishPreserveTheAuthenticationErrorCode() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String language : new String[] {"es-419", "es-PE", "es"}) {
      mvc.perform(
              post("/api/v1/sessions")
                  .header("Accept-Language", language)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"unknown@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.title").value("INVALID_CREDENTIALS"))
          .andExpect(jsonPath("$.detail").value("El correo o la contraseña son inválidos"));
    }
  }

  @Test
  void defaultAndUnsupportedLanguagesUseEnglishWithoutChangingStatusOrCode() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String language : new String[] {"", "fr-FR", "en-GB"}) {
      mvc.perform(
              post("/api/v1/sessions")
                  .header("Accept-Language", language)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"unknown@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.title").value("INVALID_CREDENTIALS"))
          .andExpect(jsonPath("$.detail").value("Email or password is invalid"));
    }
  }

  @Test
  void malformedJsonReturnsLocalizedValidationWithoutReflectingThePayload() throws Exception {
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            post("/api/v1/sessions")
                .header("Accept-Language", "es-PE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"secret"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.detail").value("Los datos de la solicitud son inválidos"));
  }

  @Test
  void thrownValidationErrorsUseTheSameLocalizedEnvelope() throws Exception {
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            post("/api/v1/sessions")
                .header("Accept-Language", "es-PE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.detail").value("Los datos de la solicitud son inválidos"));
  }
}
