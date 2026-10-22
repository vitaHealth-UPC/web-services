package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies real row reservations cover transitions and prevent duplicate consent publication. */
@SpringBootTest
@ActiveProfiles("test")
class CareLinkTransitionConcurrencyIntegrationTest {
  static class ConfirmationEvidence {
    final List<String> linkIds = new CopyOnWriteArrayList<>();

    @EventListener
    void record(CareLinkConfirmed event) {
      linkIds.add(event.careLinkId());
    }
  }

  @TestConfiguration
  static class Capture {
    @Bean
    ConfirmationEvidence confirmations() {
      return new ConfirmationEvidence();
    }
  }

  @Autowired private Clock clock;
  @Autowired private AccountRepository accounts;
  @Autowired private CareLinkCommandService commands;
  @Autowired private CareLinkRepository links;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private ConfirmationEvidence confirmations;

  @Test
  void mutationReservationsCannotOutliveAMissingCallerTransaction() {
    assertThatThrownBy(() -> links.findByIdForUpdate("missing"))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(() -> links.findByCodeForUpdate("missing"))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void competingAcceptanceWaitsForTheHeldCodeReservationAndThenObservesConsumption()
      throws Exception {
    var pending = pending();
    var reserved = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var competitorStarted = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var winner =
          workers.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .execute(
                          status -> {
                            var link =
                                links
                                    .findByCodeForUpdate(pending.linkingCode().value())
                                    .orElseThrow();
                            reserved.countDown();
                            try {
                              if (!release.await(10, TimeUnit.SECONDS))
                                throw new AssertionError("reservation release timed out");
                            } catch (InterruptedException exception) {
                              Thread.currentThread().interrupt();
                              throw new AssertionError(exception);
                            }
                            link.accept(pending.linkingCode().value(), clock.instant());
                            return links.save(link);
                          }));
      try {
        assertThat(reserved.await(10, TimeUnit.SECONDS)).isTrue();
        var competitor =
            workers.submit(
                () -> {
                  competitorStarted.countDown();
                  return acceptanceSucceeded(pending);
                });
        assertThat(competitorStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThrows(TimeoutException.class, () -> competitor.get(250, TimeUnit.MILLISECONDS));
        release.countDown();
        assertThat(winner.get(10, TimeUnit.SECONDS).status())
            .isEqualTo(CareLinkStatus.AWAITING_CONSENT);
        assertThat(competitor.get(10, TimeUnit.SECONDS)).isFalse();
      } finally {
        release.countDown();
      }
    }
    assertThat(links.findById(pending.id()).orElseThrow().linkingCode().usedAt()).isNotNull();
  }

  @Test
  void simultaneousConsentCommandsConfirmAndPublishOnce() throws Exception {
    var pending = pending();
    var accepted =
        commands.accept(
            new AcceptCareLinkCommand(pending.caregiverId(), pending.linkingCode().value()));
    if (accepted instanceof Result.Failure<CareLinkResult, ApplicationError> failure)
      throw new AssertionError("fixture acceptance rejected: " + failure.error().code());
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                return consentChanged(pending.id());
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                return consentChanged(pending.id());
              });
      start.countDown();
      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(links.findById(pending.id()).orElseThrow().isActive()).isTrue();
    assertThat(confirmations.linkIds.stream().filter(pending.id()::equals)).hasSize(1);
  }

  private boolean acceptanceSucceeded(CareLink pending) {
    var result =
        commands.accept(
            new AcceptCareLinkCommand(pending.caregiverId(), pending.linkingCode().value()));
    if (result instanceof Result.Failure<CareLinkResult, ApplicationError> failure) {
      assertThat(failure.error().code()).isEqualTo("LINKING_CODE_EXPIRED_OR_USED");
      return false;
    }
    return true;
  }

  private boolean consentChanged(String linkId) {
    return commands
        .registerConsentDecision(new RegisterConsentCommand(linkId, true))
        .toOptional()
        .orElseThrow()
        .changed();
  }

  private CareLink pending() {
    var account =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "encoded-challenge",
            clock.instant(),
            Duration.ofMinutes(15));
    account.completeVerification();
    account = accounts.save(account);
    var adult =
        commands
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    account.id(), "Rosa", LocalDate.of(1958, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var now = clock.instant();
    assertThat(now.getNano() % 1000).isZero();
    var code =
        LinkingCode.issue(
            "RACE-" + UUID.randomUUID().toString().substring(0, 8), now, Duration.ofMinutes(15));
    return links.save(CareLink.createPending(account.id(), adult.id(), code, now));
  }
}
