package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.ConsentWithdrawalEvidenceJpaRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Exercises adult-only withdrawal, retained decision evidence and immediate access invalidation.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConsentWithdrawalApiIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkRepository links;
  @Autowired private ConsentWithdrawalEvidenceJpaRepository evidence;
  @Autowired private SessionTokenService sessions;
  @Autowired private CareLinkCommandService commands;

  @Test
  void linkedAdultWithdrawsAndRetainsHistoryWhileScopedCredentialsStopWorking() throws Exception {
    var fixture = CareLinkHttpFixture.approved(context);
    var previous = links.findById(fixture.linkId()).orElseThrow();
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            delete("/api/v1/care-links/{id}/consent", fixture.linkId())
                .header("Authorization", "Bearer " + fixture.adultToken()))
        .andExpect(status().isNoContent());
    var withdrawn = links.findById(fixture.linkId()).orElseThrow();
    assertThat(withdrawn.status()).isEqualTo(CareLinkStatus.REVOKED);
    assertThat(withdrawn.isActive()).isFalse();
    assertThat(withdrawn.confirmedAt()).isEqualTo(previous.confirmedAt());
    assertThat(withdrawn.createdAt()).isEqualTo(previous.createdAt());
    var record = evidence.findById(fixture.linkId()).orElseThrow();
    assertThat(record.getApprovedAt()).isEqualTo(previous.consent().recordedAt());
    assertThat(record.getOlderAdultId()).isEqualTo(fixture.olderAdultId());
    assertThat(record.getWithdrawnAt()).isEqualTo(withdrawn.consent().recordedAt());
    assertThat(sessions.authenticate(fixture.adultToken())).isEmpty();
    assertThat(sessions.authenticate(fixture.setupToken())).isEmpty();
    assertThat(sessions.authenticate(fixture.caregiverToken())).isPresent();
    mvc.perform(
            get("/api/v1/sessions/current")
                .header("Authorization", "Bearer " + fixture.adultToken()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/care-links/authorization")
                .param("caregiverId", fixture.caregiverId())
                .param("olderAdultId", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.caregiverToken()))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
    var replay =
        commands.withdrawConsent(
            new WithdrawCareLinkConsentCommand(fixture.linkId(), fixture.olderAdultId()));
    assertThat(replay.isSuccess()).isTrue();
    assertThat(replay.toOptional().orElseThrow().changed()).isFalse();
    assertThat(evidence.findById(fixture.linkId()).orElseThrow().getWithdrawnAt())
        .isEqualTo(record.getWithdrawnAt());
  }

  @Test
  void commandReportsTheFirstWithdrawalAndItsReplayExplicitly() {
    var fixture = CareLinkHttpFixture.approved(context);
    var command = new WithdrawCareLinkConsentCommand(fixture.linkId(), fixture.olderAdultId());
    var first = commands.withdrawConsent(command);
    assertThat(first.isSuccess()).isTrue();
    assertThat(first.toOptional().orElseThrow().careLinkId()).isEqualTo(fixture.linkId());
    assertThat(first.toOptional().orElseThrow().changed()).isTrue();
    var replay = commands.withdrawConsent(command);
    assertThat(replay.isSuccess()).isTrue();
    assertThat(replay.toOptional().orElseThrow().changed()).isFalse();
  }

  @Test
  void caregiverAndForeignAdultCannotWithdrawAnotherAdultsDecision() throws Exception {
    var owner = CareLinkHttpFixture.approved(context);
    var foreign = CareLinkHttpFixture.approved(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String token : new String[] {owner.caregiverToken(), foreign.adultToken()}) {
      mvc.perform(
              delete("/api/v1/care-links/{id}/consent", owner.linkId())
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
    }
    assertThat(links.findById(owner.linkId()).orElseThrow().isActive()).isTrue();
    assertThat(evidence.existsById(owner.linkId())).isFalse();
  }

  @Test
  void restrictedOnboardingCredentialCannotInvokeConfirmedConsentWithdrawal() throws Exception {
    var setup = CareLinkHttpFixture.awaiting(context);
    assertThat(sessions.authenticate(setup.setupToken())).isPresent();
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            delete("/api/v1/care-links/{id}/consent", setup.linkId())
                .header("Authorization", "Bearer " + setup.setupToken()))
        .andExpect(status().isForbidden());
    assertThat(links.findById(setup.linkId()).orElseThrow().status())
        .isEqualTo(CareLinkStatus.AWAITING_CONSENT);
  }
}
