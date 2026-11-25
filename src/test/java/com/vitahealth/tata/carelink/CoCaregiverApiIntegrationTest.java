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

/** Several caregivers per adult, accepted by the adult, and removal of the adult's account. */
@SpringBootTest
@ActiveProfiles("test")
class CoCaregiverApiIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkRepository links;
  @Autowired private SessionTokenService sessions;

  private MockMvc mvc() {
    return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private static String invite(MockMvc mvc, CareLinkHttpFixture owner) throws Exception {
    var body =
        mvc.perform(
                post("/api/v1/care-links/{id}/caregiver-invitations", owner.linkId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"caregiverId\":\"" + owner.caregiverId() + "\"}")
                    .header("Authorization", "Bearer " + owner.caregiverToken()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.code");
  }

  private static String join(MockMvc mvc, CareLinkHttpFixture other, String code)
      throws Exception {
    var body =
        mvc.perform(
                post("/api/v1/care-links/caregiver-joins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"caregiverId\":\""
                            + other.caregiverId()
                            + "\",\"code\":\""
                            + code
                            + "\"}")
                    .header("Authorization", "Bearer " + other.caregiverToken()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("AWAITING_CONSENT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  @Test
  void anotherCaregiverJoinsOnlyAfterTheAdultAccepts() throws Exception {
    var owner = CareLinkHttpFixture.approved(context);
    var other = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    var requestId = join(mvc, other, invite(mvc, owner));

    // Not authorized before the adult decides.
    mvc.perform(
            get("/api/v1/care-links/authorization")
                .param("caregiverId", other.caregiverId())
                .param("olderAdultId", owner.olderAdultId())
                .header("Authorization", "Bearer " + other.caregiverToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));

    mvc.perform(
            get("/api/v1/care-links/caregiver-requests")
                .header("Authorization", "Bearer " + owner.adultToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].careLinkId").value(requestId))
        .andExpect(jsonPath("$[0].caregiverId").value(other.caregiverId()));

    mvc.perform(
            post("/api/v1/care-links/caregiver-requests/{id}/decision", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}")
                .header("Authorization", "Bearer " + owner.adultToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    mvc.perform(
            get("/api/v1/care-links")
                .param("caregiverId", other.caregiverId())
                .header("Authorization", "Bearer " + other.caregiverToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.olderAdultId=='" + owner.olderAdultId() + "')]").exists());
    // The original caregiver keeps their relationship.
    assertThat(links.findById(owner.linkId()).orElseThrow().isActive()).isTrue();
  }

  @Test
  void theAdultCanRejectAndCodesAreSingleUse() throws Exception {
    var owner = CareLinkHttpFixture.approved(context);
    var other = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    var code = invite(mvc, owner);
    var requestId = join(mvc, other, code);
    mvc.perform(
            post("/api/v1/care-links/caregiver-joins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\"" + other.caregiverId() + "\",\"code\":\"" + code + "\"}")
                .header("Authorization", "Bearer " + other.caregiverToken()))
        .andExpect(status().isGone());
    mvc.perform(
            post("/api/v1/care-links/caregiver-requests/{id}/decision", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":false}")
                .header("Authorization", "Bearer " + owner.adultToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));
    // A foreign adult cannot decide.
    var foreign = CareLinkHttpFixture.approved(context);
    mvc.perform(
            post("/api/v1/care-links/caregiver-requests/{id}/decision", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}")
                .header("Authorization", "Bearer " + foreign.adultToken()))
        .andExpect(status().isNotFound());
  }

  @Test
  void theAdultRemovesTheirAccountAndEveryRelationshipEnds() throws Exception {
    var owner = CareLinkHttpFixture.approved(context);
    var mvc = mvc();
    mvc.perform(
            delete("/api/v1/care-links/adult-account")
                .header("Authorization", "Bearer " + owner.adultToken()))
        .andExpect(status().isNoContent());
    assertThat(links.findById(owner.linkId()).orElseThrow().status())
        .isEqualTo(CareLinkStatus.REVOKED);
    assertThat(sessions.authenticate(owner.adultToken())).isEmpty();
    // Caregivers cannot remove an adult account.
    var other = CareLinkHttpFixture.approved(context);
    mvc.perform(
            delete("/api/v1/care-links/adult-account")
                .header("Authorization", "Bearer " + other.caregiverToken()))
        .andExpect(status().isForbidden());
  }
}
