package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.ConsentWithdrawalEvidenceJpaRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

/** Ensures a withdrawal consumer failure cannot commit partial consent or credential changes. */
@SpringBootTest
@ActiveProfiles("test")
class ConsentWithdrawalRollbackIntegrationTest {
  static class UnavailableConsumer {
    private final ConsentWithdrawalEvidenceJpaRepository evidence;
    private final SessionTokenService sessions;
    String credential;

    UnavailableConsumer(
        ConsentWithdrawalEvidenceJpaRepository evidence, SessionTokenService sessions) {
      this.evidence = evidence;
      this.sessions = sessions;
    }

    @EventListener
    @Order(2)
    void handle(CareLinkConsentWithdrawn event) {
      assertThat(evidence.existsById(event.careLinkId())).isTrue();
      assertThat(sessions.authenticate(credential)).isEmpty();
      throw new IllegalStateException("simulated withdrawal consumer failure");
    }
  }

  @TestConfiguration
  static class Failure {
    @Bean
    UnavailableConsumer unavailableConsumer(
        ConsentWithdrawalEvidenceJpaRepository evidence, SessionTokenService sessions) {
      return new UnavailableConsumer(evidence, sessions);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkCommandService commands;
  @Autowired private CareLinkRepository links;
  @Autowired private ConsentWithdrawalEvidenceJpaRepository evidence;
  @Autowired private SessionTokenService sessions;
  @Autowired private UnavailableConsumer failure;

  @Test
  void consumerFailureRollsBackCurrentConsentHistoryAndSessionRemovalTogether() {
    var fixture = CareLinkHttpFixture.approved(context);
    failure.credential = fixture.adultToken();
    assertThatThrownBy(
            () ->
                commands.withdrawConsent(
                    new WithdrawCareLinkConsentCommand(fixture.linkId(), fixture.olderAdultId())))
        .isInstanceOf(IllegalStateException.class);
    assertThat(links.findById(fixture.linkId()).orElseThrow().isActive()).isTrue();
    assertThat(evidence.existsById(fixture.linkId())).isFalse();
    assertThat(sessions.authenticate(fixture.adultToken())).isPresent();
  }
}
