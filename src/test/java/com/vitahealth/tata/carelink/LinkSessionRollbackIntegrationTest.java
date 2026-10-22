package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/** Verifies extracted handlers retain rollback when issuing the onboarding credential fails. */
@SpringBootTest
@ActiveProfiles("test")
class LinkSessionRollbackIntegrationTest {
  @TestConfiguration
  static class FailedIssuance {
    @Bean
    @Primary
    LinkSessionPort unavailableSessionIssuer() {
      return link -> {
        throw new IllegalStateException("simulated session issuance failure");
      };
    }
  }

  @Autowired private AccountRepository accounts;
  @Autowired private CareLinkCommandService commands;
  @Autowired private LinkAuthenticationCommandService authentication;
  @Autowired private CareLinkRepository links;

  @Test
  void failureToIssueOnboardingAccessDoesNotConsumeTheLinkingCode() {
    var caregiver =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "encoded-challenge",
            Instant.now(),
            Duration.ofMinutes(15));
    caregiver.completeVerification();
    caregiver = accounts.save(caregiver);
    var profile =
        commands
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver.id(), "Rosa", LocalDate.of(1958, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var pending =
        commands
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver.id(), profile.id()))
            .toOptional()
            .orElseThrow();
    var command = new AcceptCareLinkCommand(caregiver.id(), pending.linkingCode());
    assertThatThrownBy(() -> authentication.accept(command))
        .isInstanceOf(IllegalStateException.class);
    var restored = links.findById(pending.id()).orElseThrow();
    assertThat(restored.status()).isEqualTo(CareLinkStatus.PENDING);
    assertThat(restored.linkingCode().usedAt()).isNull();
  }

  @Test
  void failureToIssueAdultAccessRollsBackRecordedApprovalAndConfirmation() {
    var caregiver =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "encoded-challenge",
            Instant.now(),
            Duration.ofMinutes(15));
    caregiver.completeVerification();
    caregiver = accounts.save(caregiver);
    var profile =
        commands
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver.id(), "Rosa", LocalDate.of(1958, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var pending =
        commands
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver.id(), profile.id()))
            .toOptional()
            .orElseThrow();
    commands
        .accept(new AcceptCareLinkCommand(caregiver.id(), pending.linkingCode()))
        .toOptional()
        .orElseThrow();
    assertThatThrownBy(
            () -> authentication.registerConsent(new RegisterConsentCommand(pending.id(), true)))
        .isInstanceOf(IllegalStateException.class);
    var restored = links.findById(pending.id()).orElseThrow();
    assertThat(restored.status()).isEqualTo(CareLinkStatus.AWAITING_CONSENT);
    assertThat(restored.consent().recordedAt()).isNull();
    assertThat(restored.confirmedAt()).isNull();
  }
}
