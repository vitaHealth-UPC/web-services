package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Ensures absent or malformed consent cannot become a stored rejection through default values. */
@SpringBootTest
@ActiveProfiles("test")
class ExplicitConsentDecisionApiIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkRepository links;

  @Test
  void invalidDecisionLeavesOnboardingAndItsCredentialUsable() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String payload : new String[] {"{}", "{\"accepted\":null}", "{"}) {
      mvc.perform(
              post("/api/v1/care-links/{id}/consent", fixture.linkId())
                  .header("Authorization", "Bearer " + fixture.setupToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(payload))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
      var restored = links.findById(fixture.linkId()).orElseThrow();
      assertThat(restored.status()).isEqualTo(CareLinkStatus.AWAITING_CONSENT);
      assertThat(restored.consent().recordedAt()).isNull();
    }
    mvc.perform(
            post("/api/v1/care-links/{id}/consent", fixture.linkId())
                .header("Authorization", "Bearer " + fixture.setupToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));
    assertThat(links.findById(fixture.linkId()).orElseThrow().consent().recordedAt()).isNotNull();
  }
}
