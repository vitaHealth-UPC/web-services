package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.configuration.CareLinkCodeAllocationInitializer;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Exercises real transactional allocation with controlled candidate collisions and observable time.
 */
@SpringBootTest
@ActiveProfiles("test")
class LinkingCodeAllocationIntegrationTest {
  /** Advances monotonically so previous tests' historical codes never become live again. */
  static class AllocationClock extends Clock {
    private Instant now = Instant.parse("2026-10-13T12:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  /** Controls randomness only; all reservation, uniqueness and persistence code remains real. */
  static class Candidates implements LinkingCodeGenerator {
    private final ThreadLocal<Supplier<String>> sequence = new ThreadLocal<>();

    void using(Supplier<String> values) {
      sequence.set(values);
    }

    void using(String... values) {
      var pending = new ArrayDeque<>(Arrays.asList(values));
      var fallback = values[values.length - 1];
      using(() -> pending.isEmpty() ? fallback : pending.removeFirst());
    }

    @Override
    public String generate() {
      return sequence.get().get();
    }
  }

  @TestConfiguration
  static class ControlledIssuance {
    @Bean
    @Primary
    AllocationClock allocationClock() {
      return new AllocationClock();
    }

    @Bean
    @Primary
    Candidates candidates() {
      return new Candidates();
    }
  }

  record Profile(String caregiverId, String adultId) {}

  @Autowired private AccountRepository accounts;
  @Autowired private CareLinkCommandService commands;
  @Autowired private CareLinkRepository links;
  @Autowired private CareLinkJpaRepository rows;
  @Autowired private Candidates candidates;
  @Autowired private AllocationClock clock;
  @Autowired private CareLinkCodeAllocationInitializer initializer;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private WebApplicationContext context;
  @Autowired private SessionTokenService sessions;

  @BeforeEach
  void advancePastEarlierFixtures() {
    clock.advance(Duration.ofDays(1));
  }

  @Test
  void occupiedCandidateIsSkippedAndBoundedFailureCreatesNoPartialRelationship() throws Exception {
    var existing = profile();
    seed(existing, "TATA-7401");
    var request = profile();
    candidates.using("TATA-7401", "TATA-7402");
    var allocated =
        commands
            .generateLinkingCode(
                new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId()))
            .toOptional()
            .orElseThrow();
    assertThat(allocated.linkingCode()).isEqualTo("TATA-7402");
    assertThat(links.findByCode("TATA-7401").orElseThrow().caregiverId())
        .isEqualTo(existing.caregiverId());
    var exhaustion = profile();
    long before = rows.count();
    var attempts = new AtomicInteger();
    candidates.using(
        () -> {
          attempts.incrementAndGet();
          return "TATA-7401";
        });
    var failure =
        requireFailure(
            commands.generateLinkingCode(
                new GenerateLinkingCodeCommand(exhaustion.caregiverId(), exhaustion.adultId())));
    assertThat(failure.code()).isEqualTo("LINKING_CODE_UNAVAILABLE");
    assertThat(attempts.get()).isEqualTo(64);
    assertThat(rows.count()).isEqualTo(before);
    var token = sessions.issue(exhaustion.caregiverId()).accessToken();
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            post("/api/v1/care-links/linking-codes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\""
                        + exhaustion.caregiverId()
                        + "\",\"olderAdultId\":\""
                        + exhaustion.adultId()
                        + "\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.title").value("LINKING_CODE_UNAVAILABLE"));
    assertThat(rows.count()).isEqualTo(before);
  }

  @Test
  void databaseConstraintRejectsAnActiveDuplicateOutsideTheIssuerProtocol() {
    var original = seed(profile(), "TATA-7501");
    var duplicate = profile();
    var failure =
        assertThrows(DataIntegrityViolationException.class, () -> seed(duplicate, "TATA-7501"));
    assertThat(UniqueConstraintClassifier.matches(failure, "uk_care_link_reserved_code")).isTrue();
    assertThat(links.findByCode("TATA-7501").orElseThrow().id()).isEqualTo(original.id());
  }

  @Test
  void usedCodeCanBeReallocatedWhileHistoricalEvidenceRemainsReadable() {
    var original = seed(profile(), "TATA-7601");
    commands
        .accept(new AcceptCareLinkCommand(original.caregiverId(), "TATA-7601"))
        .toOptional()
        .orElseThrow();
    var next = profile();
    candidates.using("TATA-7601");
    var allocated =
        commands
            .generateLinkingCode(new GenerateLinkingCodeCommand(next.caregiverId(), next.adultId()))
            .toOptional()
            .orElseThrow();
    assertThat(allocated.id()).isNotEqualTo(original.id());
    assertThat(links.findById(original.id()).orElseThrow().linkingCode().usedAt()).isNotNull();
    assertThat(links.findByCode("TATA-7601").orElseThrow().id()).isEqualTo(allocated.id());
  }

  @Test
  void expiredReservationIsFreedWithoutOverwritingThePreviousCodeEvidence() {
    var original = seed(profile(), "TATA-7701");
    var expiration = original.linkingCode().expiresAt();
    clock.advance(Duration.ofMinutes(16));
    var next = profile();
    candidates.using("TATA-7701");
    var allocated =
        commands
            .generateLinkingCode(new GenerateLinkingCodeCommand(next.caregiverId(), next.adultId()))
            .toOptional()
            .orElseThrow();
    assertThat(links.findById(original.id()).orElseThrow().linkingCode().expiresAt())
        .isEqualTo(expiration);
    assertThat(links.findById(original.id()).orElseThrow().linkingCode().usedAt()).isNull();
    assertThat(links.findByCode("TATA-7701").orElseThrow().id()).isEqualTo(allocated.id());
  }

  @Test
  void competingIssuerWaitsForNamespaceAndRetriesTheNowOccupiedCandidate() throws Exception {
    var firstProfile = profile();
    var secondProfile = profile();
    var firstCandidateEntered = new CountDownLatch(1);
    var releaseFirst = new CountDownLatch(1);
    var secondStarted = new CountDownLatch(1);
    var secondCandidates = new AtomicInteger();
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                candidates.using(
                    () -> {
                      firstCandidateEntered.countDown();
                      try {
                        if (!releaseFirst.await(10, TimeUnit.SECONDS))
                          throw new AssertionError("issuer release timed out");
                      } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                      }
                      return "TATA-7801";
                    });
                return commands
                    .generateLinkingCode(
                        new GenerateLinkingCodeCommand(
                            firstProfile.caregiverId(), firstProfile.adultId()))
                    .toOptional()
                    .orElseThrow();
              });
      try {
        assertThat(firstCandidateEntered.await(10, TimeUnit.SECONDS)).isTrue();
        var second =
            workers.submit(
                () -> {
                  candidates.using(
                      () -> secondCandidates.getAndIncrement() == 0 ? "TATA-7801" : "TATA-7802");
                  secondStarted.countDown();
                  return commands
                      .generateLinkingCode(
                          new GenerateLinkingCodeCommand(
                              secondProfile.caregiverId(), secondProfile.adultId()))
                      .toOptional()
                      .orElseThrow();
                });
        assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS));
        assertThat(secondCandidates.get()).isZero();
        releaseFirst.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).linkingCode()).isEqualTo("TATA-7801");
        assertThat(second.get(10, TimeUnit.SECONDS).linkingCode()).isEqualTo("TATA-7802");
        assertThat(secondCandidates.get()).isEqualTo(2);
      } finally {
        releaseFirst.countDown();
      }
    }
  }

  @Test
  void initializationBackfillsAnUnreservedLiveRowWithoutReplacingHistory() {
    var historical = seed(profile(), "TATA-7901");
    jdbc.update("update care_links set reserved_linking_code = null where id = ?", historical.id());
    assertThat(links.isCodeReserved("TATA-7901")).isFalse();
    initializer.initialize();
    assertThat(links.isCodeReserved("TATA-7901")).isTrue();
    assertThat(links.findById(historical.id()).orElseThrow().createdAt())
        .isEqualTo(historical.createdAt());
  }

  @Test
  void startupReconciliationRefusesAmbiguousLiveCodesWithoutChoosingOrDeletingHistory() {
    var original = seed(profile(), "TATA-7991");
    jdbc.update("update care_links set reserved_linking_code = null where id = ?", original.id());
    var duplicateId = UUID.randomUUID().toString();
    jdbc.update(
        """
        insert into care_links (id, caregiver_id, older_adult_id, status, linking_code,
          code_expires_at, code_used_at, consent_granted, consent_recorded_at,
          confirmed_at, created_at, updated_at)
        select ?, caregiver_id, older_adult_id, status, linking_code,
          code_expires_at, code_used_at, consent_granted, consent_recorded_at,
          confirmed_at, created_at, updated_at from care_links where id = ?
        """,
        duplicateId,
        original.id());
    var failure = assertThrows(DataIntegrityViolationException.class, initializer::initialize);
    assertThat(UniqueConstraintClassifier.matches(failure, "uk_care_link_reserved_code")).isTrue();
    assertThat(links.isCodeReserved("TATA-7991")).isFalse();
    assertThat(links.findById(original.id())).isPresent();
    assertThat(links.findById(duplicateId)).isPresent();
  }

  @Test
  void retryReturnsSamePendingEvidenceWithoutGeneratingAnotherCandidate() {
    var request = profile();
    var generated = new AtomicInteger();
    candidates.using(
        () -> {
          generated.incrementAndGet();
          return "TATA-8101";
        });
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    var original = commands.generateLinkingCode(command).toOptional().orElseThrow();
    long count = rows.count();
    var retry = commands.generateLinkingCode(command).toOptional().orElseThrow();
    assertThat(retry.id()).isEqualTo(original.id());
    assertThat(retry.linkingCode()).isEqualTo(original.linkingCode());
    assertThat(retry.codeExpiresAt()).isEqualTo(original.codeExpiresAt());
    assertThat(rows.count()).isEqualTo(count);
    assertThat(generated.get()).isEqualTo(1);
  }

  @Test
  void exactExpiryAllowsANewRequestWithoutChangingOldHistory() {
    var request = profile();
    candidates.using("TATA-8201", "TATA-8202");
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    var original = commands.generateLinkingCode(command).toOptional().orElseThrow();
    clock.advance(Duration.ofMinutes(15));
    var replacement = commands.generateLinkingCode(command).toOptional().orElseThrow();
    assertThat(replacement.id()).isNotEqualTo(original.id());
    assertThat(replacement.linkingCode()).isEqualTo("TATA-8202");
    var historical = links.findById(original.id()).orElseThrow();
    assertThat(historical.linkingCode().expiresAt()).isEqualTo(original.codeExpiresAt());
    assertThat(historical.linkingCode().usedAt()).isNull();
  }

  @Test
  void acceptedOrConfirmedPairCannotGenerateAnotherRelationship() throws Exception {
    var request = profile();
    candidates.using("TATA-8301");
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    var original = commands.generateLinkingCode(command).toOptional().orElseThrow();
    commands
        .accept(new AcceptCareLinkCommand(request.caregiverId(), original.linkingCode()))
        .toOptional()
        .orElseThrow();
    long count = rows.count();
    var failure = requireFailure(commands.generateLinkingCode(command));
    assertThat(failure.code()).isEqualTo("CARE_LINK_ALREADY_EXISTS");
    clock.advance(Duration.ofMinutes(16));
    var token = sessions.issue(request.caregiverId()).accessToken();
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            post("/api/v1/care-links/linking-codes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\""
                        + request.caregiverId()
                        + "\",\"olderAdultId\":\""
                        + request.adultId()
                        + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("CARE_LINK_ALREADY_EXISTS"));
    commands
        .registerConsent(new RegisterConsentCommand(original.id(), true))
        .toOptional()
        .orElseThrow();
    failure = requireFailure(commands.generateLinkingCode(command));
    assertThat(failure.code()).isEqualTo("CARE_LINK_ALREADY_EXISTS");
    assertThat(rows.count()).isEqualTo(count);
  }

  @Test
  void rejectedPairAllowsANewIdentityAndPreservesTheDecision() {
    var request = profile();
    candidates.using("TATA-8401", "TATA-8402");
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    var original = commands.generateLinkingCode(command).toOptional().orElseThrow();
    commands
        .accept(new AcceptCareLinkCommand(request.caregiverId(), original.linkingCode()))
        .toOptional()
        .orElseThrow();
    commands
        .registerConsent(new RegisterConsentCommand(original.id(), false))
        .toOptional()
        .orElseThrow();
    var replacement = commands.generateLinkingCode(command).toOptional().orElseThrow();
    assertThat(replacement.id()).isNotEqualTo(original.id());
    assertThat(links.findById(original.id()).orElseThrow().consent().recordedAt()).isNotNull();
  }

  @Test
  void databaseConstraintRejectsDuplicateLivePairEvenWithDifferentCodes() {
    var request = profile();
    var original = seed(request, "TATA-8501");
    var failure =
        assertThrows(DataIntegrityViolationException.class, () -> seed(request, "TATA-8502"));
    assertThat(UniqueConstraintClassifier.matches(failure, "uk_care_link_reserved_members"))
        .isTrue();
    assertThat(links.findById(original.id())).isPresent();
  }

  @Test
  void concurrentRetriesForSamePairShareOneCodeAndIdentity() throws Exception {
    var request = profile();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var secondStarted = new CountDownLatch(1);
    var secondCandidates = new AtomicInteger();
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                candidates.using(
                    () -> {
                      entered.countDown();
                      try {
                        if (!release.await(10, TimeUnit.SECONDS))
                          throw new AssertionError("issuer release timed out");
                      } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                      }
                      return "TATA-8601";
                    });
                return commands.generateLinkingCode(command).toOptional().orElseThrow();
              });
      try {
        assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
        var second =
            workers.submit(
                () -> {
                  candidates.using(
                      () -> {
                        secondCandidates.incrementAndGet();
                        return "TATA-8602";
                      });
                  secondStarted.countDown();
                  return commands.generateLinkingCode(command).toOptional().orElseThrow();
                });
        assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS));
        release.countDown();
        var original = first.get(10, TimeUnit.SECONDS);
        var retry = second.get(10, TimeUnit.SECONDS);
        assertThat(retry.id()).isEqualTo(original.id());
        assertThat(retry.linkingCode()).isEqualTo(original.linkingCode());
        assertThat(secondCandidates.get()).isZero();
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void initializationRefusesAmbiguousMemberPairsWithoutDeletingHistory() {
    var original = seed(profile(), "TATA-8701");
    jdbc.update(
        "update care_links set reserved_caregiver_id = null, reserved_older_adult_id = null where"
            + " id = ?",
        original.id());
    var duplicate = UUID.randomUUID().toString();
    jdbc.update(
        """
        insert into care_links (id, caregiver_id, older_adult_id, status, linking_code,
          code_expires_at, code_used_at, consent_granted, consent_recorded_at,
          confirmed_at, created_at, updated_at)
        select ?, caregiver_id, older_adult_id, status, 'TATA-8702',
          code_expires_at, code_used_at, consent_granted, consent_recorded_at,
          confirmed_at, created_at, updated_at from care_links where id = ?
        """,
        duplicate,
        original.id());
    var failure = assertThrows(DataIntegrityViolationException.class, initializer::initialize);
    assertThat(UniqueConstraintClassifier.matches(failure, "uk_care_link_reserved_members"))
        .isTrue();
    assertThat(links.findById(original.id())).isPresent();
    assertThat(links.findById(duplicate)).isPresent();
  }

  @Test
  void withdrawalReleasesThePairWithoutErasingHistoricalConfirmation() {
    var request = profile();
    candidates.using("TATA-8801", "TATA-8802");
    var command = new GenerateLinkingCodeCommand(request.caregiverId(), request.adultId());
    var original = commands.generateLinkingCode(command).toOptional().orElseThrow();
    commands
        .accept(new AcceptCareLinkCommand(request.caregiverId(), original.linkingCode()))
        .toOptional()
        .orElseThrow();
    commands
        .registerConsent(new RegisterConsentCommand(original.id(), true))
        .toOptional()
        .orElseThrow();
    commands.withdrawConsent(new WithdrawCareLinkConsentCommand(original.id(), request.adultId()));
    var replacement = commands.generateLinkingCode(command).toOptional().orElseThrow();
    assertThat(replacement.id()).isNotEqualTo(original.id());
    var historical = links.findById(original.id()).orElseThrow();
    assertThat(historical.confirmedAt()).isNotNull();
    assertThat(historical.isActive()).isFalse();
  }

  @Test
  void initializationRestoresBothColumnsOfAPartialPairReservation() {
    var original = seed(profile(), "TATA-8901");
    jdbc.update("update care_links set reserved_older_adult_id = null where id = ?", original.id());
    initializer.initialize();
    var restored =
        jdbc.queryForMap(
            "select reserved_caregiver_id, reserved_older_adult_id from care_links where id = ?",
            original.id());
    assertThat(restored.get("reserved_caregiver_id")).isEqualTo(original.caregiverId());
    assertThat(restored.get("reserved_older_adult_id")).isEqualTo(original.olderAdultId());
  }

  private CareLink seed(Profile profile, String code) {
    var now = clock.instant();
    return links.save(
        CareLink.createPending(
            profile.caregiverId(),
            profile.adultId(),
            LinkingCode.issue(code, now, Duration.ofMinutes(15)),
            now));
  }

  private Profile profile() {
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
    return new Profile(account.id(), adult.id());
  }

  private static ApplicationError requireFailure(Result<CareLinkResult, ApplicationError> result) {
    assertThat(result.isFailure()).isTrue();
    var failure = (Result.Failure<CareLinkResult, ApplicationError>) result;
    return failure.error();
  }
}
