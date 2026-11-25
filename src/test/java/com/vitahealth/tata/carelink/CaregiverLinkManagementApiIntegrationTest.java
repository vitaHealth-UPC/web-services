package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Caregiver-side relationship management: ending a link, issuing an adult sign-in code for a
 * new device and the signed-in state shown in the family list.
 */
@SpringBootTest
@ActiveProfiles("test")
class CaregiverLinkManagementApiIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkRepository links;
  @Autowired private SessionTokenService sessions;

  private MockMvc mvc() {
    return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void caregiverEndsOwnLinkAndAdultAccessStops() throws Exception {
    var fixture = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    mvc.perform(
            get("/api/v1/care-links")
                .param("caregiverId", fixture.caregiverId())
                .header("Authorization", "Bearer " + fixture.caregiverToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].adultSignedIn").value(true));
    mvc.perform(
            delete("/api/v1/care-links/{id}", fixture.linkId())
                .param("caregiverId", fixture.caregiverId())
                .header("Authorization", "Bearer " + fixture.caregiverToken()))
        .andExpect(status().isNoContent());
    assertThat(links.findById(fixture.linkId()).orElseThrow().status())
        .isEqualTo(CareLinkStatus.REVOKED);
    assertThat(sessions.authenticate(fixture.adultToken())).isEmpty();
    // Repeating it is harmless.
    mvc.perform(
            delete("/api/v1/care-links/{id}", fixture.linkId())
                .param("caregiverId", fixture.caregiverId())
                .header("Authorization", "Bearer " + fixture.caregiverToken()))
        .andExpect(status().isNoContent());
  }

  @Test
  void anotherCaregiverCannotEndTheLink() throws Exception {
    var owner = CareLinkHttpFixture.approved(context);
    var other = CareLinkHttpFixture.approved(context);
    mvc()
        .perform(
            delete("/api/v1/care-links/{id}", owner.linkId())
                .param("caregiverId", other.caregiverId())
                .header("Authorization", "Bearer " + other.caregiverToken()))
        .andExpect(status().is4xxClientError());
    assertThat(links.findById(owner.linkId()).orElseThrow().isActive()).isTrue();
  }

  @Test
  void adultSignsInOnANewDeviceWithASingleUseCode() throws Exception {
    var fixture = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    var issued =
        mvc.perform(
                post("/api/v1/care-links/{id}/access-codes", fixture.linkId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"caregiverId\":\"" + fixture.caregiverId() + "\"}")
                    .header("Authorization", "Bearer " + fixture.caregiverToken()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String code = JsonPath.read(issued, "$.code");
    assertThat(code).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");

    var redeemed =
        mvc.perform(
                post("/api/v1/care-links/access-code-redemptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code.toLowerCase() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.olderAdultId").value(fixture.olderAdultId()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    boolean pin = JsonPath.read(redeemed, "$.pinRegistered");
    if (!pin) {
      String token = JsonPath.read(redeemed, "$.accessToken");
      assertThat(sessions.authenticate(token)).isPresent();
    }
    // Single use.
    mvc.perform(
            post("/api/v1/care-links/access-code-redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isGone());
  }

  @Test
  void aNewCodeReplacesTheUnusedOneAndUnknownCodesFail() throws Exception {
    var fixture = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    String first = issue(mvc, fixture);
    String second = issue(mvc, fixture);
    assertThat(second).isNotEqualTo(first);
    mvc.perform(
            post("/api/v1/care-links/access-code-redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + first + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/care-links/access-code-redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + second + "\"}"))
        .andExpect(status().isOk());
  }

  private static String issue(MockMvc mvc, CareLinkHttpFixture fixture)
      throws Exception {
    var body =
        mvc.perform(
                post("/api/v1/care-links/{id}/access-codes", fixture.linkId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"caregiverId\":\"" + fixture.caregiverId() + "\"}")
                    .header("Authorization", "Bearer " + fixture.caregiverToken()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.code");
  }
}
