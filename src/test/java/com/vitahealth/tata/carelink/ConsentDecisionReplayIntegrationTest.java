package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.application.internal.commandservices.ConfirmCareLinkCommandHandler;
import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.ConfirmCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/** Exercises decision replays through real locks, credentials and authorized HTTP requests. */
@SpringBootTest
@ActiveProfiles("test")
class ConsentDecisionReplayIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkCommandService commands;
  @Autowired private LinkAuthenticationCommandService authentication;
  @Autowired private CareLinkRepository links;
  @Autowired private AccountSessionJpaRepository sessions;
  @Autowired private SessionTokenService tokens;
  @Autowired private ConfirmCareLinkCommandHandler confirmation;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private Clock clock;

  @Test
  void confirmedReplayPreservesEvidenceAndDoesNotIssueAnotherCredential() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var first =
        authentication
            .registerConsent(new RegisterConsentCommand(fixture.linkId(), true))
            .toOptional()
            .orElseThrow();
    long count = sessions.count();
    var replay =
        authentication
            .registerConsent(new RegisterConsentCommand(fixture.linkId(), true))
            .toOptional()
            .orElseThrow();
    assertThat(replay.accessToken()).isNull();
    assertThat(replay.link().confirmedAt()).isEqualTo(first.link().confirmedAt());
    assertThat(replay.link().consentRecordedAt()).isEqualTo(first.link().consentRecordedAt());
    assertThat(sessions.count()).isEqualTo(count);
    assertThat(tokens.authenticate(first.accessToken())).isPresent();
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            post("/api/v1/care-links/{id}/consent", fixture.linkId())
                .header("Authorization", "Bearer " + first.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.accessToken").doesNotExist());
    assertThat(sessions.count()).isEqualTo(count);
    mvc.perform(
            post("/api/v1/care-links/{id}/consent", fixture.linkId())
                .header("Authorization", "Bearer " + fixture.setupToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void usedCodeRetryCannotIssueAnotherOnboardingCredential() {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var link = links.findById(fixture.linkId()).orElseThrow();
    long count = sessions.count();
    var failure =
        authentication.accept(
            new AcceptCareLinkCommand(fixture.caregiverId(), link.linkingCode().value()));
    assertThat(failure.isFailure()).isTrue();
    assertThat(
            ((Result.Failure<AuthenticatedCareLinkResult, ApplicationError>) failure)
                .error()
                .code())
        .isEqualTo("LINKING_CODE_EXPIRED_OR_USED");
    assertThat(sessions.count()).isEqualTo(count);
    assertThat(tokens.authenticate(fixture.setupToken())).isPresent();
  }

  @Test
  void rejectedReplayPreservesOriginalDecisionWithoutReactivatingRelationship() {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var first =
        commands
            .registerConsentDecision(new RegisterConsentCommand(fixture.linkId(), false))
            .toOptional()
            .orElseThrow();
    var replay =
        commands
            .registerConsentDecision(new RegisterConsentCommand(fixture.linkId(), false))
            .toOptional()
            .orElseThrow();
    assertThat(first.changed()).isTrue();
    assertThat(replay.changed()).isFalse();
    assertThat(replay.link().consentRecordedAt()).isEqualTo(first.link().consentRecordedAt());
    assertThat(links.findById(fixture.linkId()).orElseThrow().isActive()).isFalse();
  }

  @Test
  void oppositeDecisionCannotReplaceRecordedApprovalOrRejection() {
    for (boolean accepted : new boolean[] {true, false}) {
      var fixture = CareLinkHttpFixture.awaiting(context);
      var before =
          commands
              .registerConsent(new RegisterConsentCommand(fixture.linkId(), accepted))
              .toOptional()
              .orElseThrow();
      var failure =
          commands.registerConsent(new RegisterConsentCommand(fixture.linkId(), !accepted));
      assertThat(failure.isFailure()).isTrue();
      assertThat(((Result.Failure<CareLinkResult, ApplicationError>) failure).error().code())
          .isEqualTo("CONSENT_REQUIRED");
      var after = links.findById(fixture.linkId()).orElseThrow();
      assertThat(after.consent().recordedAt()).isEqualTo(before.consentRecordedAt());
      assertThat(after.status()).isEqualTo(before.status());
    }
  }

  @Test
  void confirmationRequiresTheConsentWorkflowTransaction() {
    var fixture = CareLinkHttpFixture.awaiting(context);
    assertThatThrownBy(
            () ->
                confirmation.handle(new ConfirmCareLinkCommand(fixture.linkId(), clock.instant())))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void confirmationCannotBypassAnUnrecordedAdultDecision() {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var transaction = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status ->
                        confirmation.handle(
                            new ConfirmCareLinkCommand(fixture.linkId(), clock.instant()))))
        .isInstanceOf(CareLinkApplicationException.class);
    assertThat(links.findById(fixture.linkId()).orElseThrow().consent().recordedAt()).isNull();
  }
}
