package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.identitysubscription.application.internal.commandservices.CompletePasswordRecoveryCommandHandler;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.RevokeAccountSessionsPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.CompletePasswordRecoveryCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.*;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class CompletePasswordRecoveryIntegrationTest {
  @Autowired CompletePasswordRecoveryCommandHandler handler;
  @Autowired AccountRepository accounts;
  @Autowired PasswordRecoveryRepository recoveries;
  @Autowired PasswordHasher hasher;
  @Autowired RevokeAccountSessionsPort revocation;
  @Autowired SessionTokenService tokens;
  @Autowired PlatformTransactionManager manager;
  @Autowired AccountJpaRepository accountRows;
  @Autowired PasswordRecoveryJpaRepository recoveryRows;
  @Autowired AccountSessionJpaRepository sessionRows;
  @Autowired Clock clock;
  private final List<String> created = new ArrayList<>();

  private Account fixture() {
    var now = clock.instant();
    var value =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            hasher.hash("old-password"),
            "verification",
            now,
            Duration.ofMinutes(15));
    value.completeVerification();
    value = accounts.save(value);
    created.add(value.id());
    final var id = value.id();
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s ->
                recoveries.save(
                    id,
                    PasswordRecoveryChallenge.issue(
                        hasher.hash("123456"), now, Duration.ofMinutes(10))));
    return value;
  }

  private CompletePasswordRecoveryCommand command(Account account, String code, String password) {
    return new CompletePasswordRecoveryCommand(account.email().value(), code, password);
  }

  @AfterEach
  void removeCommittedFixtures() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              for (var id : created) {
                sessionRows.revokeCaregiverSessions(id);
                recoveryRows.findById(id).ifPresent(recoveryRows::delete);
                accountRows.deleteById(id);
              }
            });
  }

  @Test
  void resetsPasswordRevokesAllOwnedSessionsAndRejectsReplayWithoutAffectingAnotherAccount() {
    var owner = fixture();
    var other = fixture();
    var first = tokens.issue(owner.id()).accessToken();
    var second = tokens.issue(owner.id()).accessToken();
    var unrelated = tokens.issue(other.id()).accessToken();
    assertTrue(handler.handle(command(owner, "123456", "new-password")).isSuccess());
    var saved = accounts.findById(owner.id()).orElseThrow();
    assertTrue(hasher.matches("new-password", saved.passwordHash()));
    assertFalse(hasher.matches("old-password", saved.passwordHash()));
    assertEquals(owner.subscriptionRenewsAt(), saved.subscriptionRenewsAt());
    assertEquals(owner.currentPlanCode(), saved.currentPlanCode());
    assertNotNull(recoveries.findByAccountId(owner.id()).orElseThrow().consumedAt());
    assertTrue(tokens.authenticate(first).isEmpty());
    assertTrue(tokens.authenticate(second).isEmpty());
    assertTrue(tokens.authenticate(unrelated).isPresent());
    var fresh = tokens.issue(owner.id()).accessToken();
    assertTrue(handler.handle(command(owner, "123456", "another-password")).isFailure());
    assertTrue(tokens.authenticate(fresh).isPresent());
    assertTrue(
        hasher.matches("new-password", accounts.findById(owner.id()).orElseThrow().passwordHash()));
  }

  @Test
  void incorrectCodesCommitAttemptLimitsWithoutChangingPasswordOrRevokingSessions() {
    var owner = fixture();
    var token = tokens.issue(owner.id()).accessToken();
    for (int attempt = 1; attempt <= 5; attempt++) {
      assertTrue(handler.handle(command(owner, "000000", "new-password")).isFailure());
      assertEquals(attempt, recoveries.findByAccountId(owner.id()).orElseThrow().failedAttempts());
    }
    assertTrue(handler.handle(command(owner, "123456", "new-password")).isFailure());
    assertTrue(
        hasher.matches("old-password", accounts.findById(owner.id()).orElseThrow().passwordHash()));
    assertTrue(tokens.authenticate(token).isPresent());
    assertNull(recoveries.findByAccountId(owner.id()).orElseThrow().consumedAt());
  }

  @Test
  void failedSessionRevocationRollsBackPasswordConsumptionAndSessionDeletion() {
    var owner = fixture();
    var token = tokens.issue(owner.id()).accessToken();
    var failing =
        new CompletePasswordRecoveryCommandHandler(
            accounts,
            recoveries,
            hasher,
            id -> {
              revocation.revokeCaregiverSessions(id);
              throw new IllegalStateException("Simulated revocation failure");
            },
            clock);
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(manager)
                .execute(s -> failing.handle(command(owner, "123456", "new-password"))));
    assertTrue(
        hasher.matches("old-password", accounts.findById(owner.id()).orElseThrow().passwordHash()));
    assertNull(recoveries.findByAccountId(owner.id()).orElseThrow().consumedAt());
    assertTrue(tokens.authenticate(token).isPresent());
  }

  @Test
  void unsupportedPasswordDoesNotConsumeValidRecoveryEvidence() {
    var owner = fixture();
    assertTrue(handler.handle(command(owner, "123456", "short")).isFailure());
    assertNull(recoveries.findByAccountId(owner.id()).orElseThrow().consumedAt());
    assertEquals(0, recoveries.findByAccountId(owner.id()).orElseThrow().failedAttempts());
    assertTrue(handler.handle(command(owner, "123456", "x".repeat(73))).isFailure());
    assertNull(recoveries.findByAccountId(owner.id()).orElseThrow().consumedAt());
  }
}
