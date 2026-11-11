package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.*;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PasswordRecoveryJpaRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class PasswordRecoveryPersistenceIntegrationTest {
  @Autowired AccountRepository accounts;
  @Autowired PasswordRecoveryRepository recoveries;
  @Autowired PlatformTransactionManager manager;
  @Autowired AccountJpaRepository accountRows;
  @Autowired PasswordRecoveryJpaRepository recoveryRows;
  private final List<String> createdAccounts = new ArrayList<>();

  @AfterEach
  void removeOnlyCommittedFixturesCreatedByThisTest() {
    transaction()
        .executeWithoutResult(
            s -> {
              for (var id : createdAccounts) {
                recoveryRows.findById(id).ifPresent(recoveryRows::delete);
                accountRows.deleteById(id);
              }
            });
  }

  private final Instant now = Instant.parse("2026-10-26T12:00:00Z");

  private PasswordRecoveryChallenge challenge() {
    return PasswordRecoveryChallenge.issue("encoded-recovery", now, Duration.ofMinutes(10));
  }

  private String account() {
    var value =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "verification",
            now,
            Duration.ofMinutes(15));
    value.completeVerification();
    var id = accounts.save(value).id();
    createdAccounts.add(id);
    return id;
  }

  private TransactionTemplate transaction() {
    return new TransactionTemplate(manager);
  }

  private void issue(String id) {
    transaction().executeWithoutResult(s -> recoveries.save(id, challenge()));
  }

  @Test
  void failedAttemptsSurviveASeparateReadWithoutChangingAccountVerification() {
    var id = account();
    issue(id);
    transaction()
        .executeWithoutResult(
            s -> {
              var current = recoveries.findByAccountIdForUpdate(id).orElseThrow();
              recoveries.save(id, current.attempt(false, now.plusSeconds(1)).challenge());
            });
    var stored = recoveries.findByAccountId(id).orElseThrow();
    assertEquals(1, stored.failedAttempts());
    assertNull(stored.consumedAt());
    assertEquals("encoded-recovery", stored.codeHash());
    assertTrue(accounts.findById(id).orElseThrow().canAuthenticate());
    assertNull(accounts.findById(id).orElseThrow().verificationCodeHash());
  }

  @Test
  void rollbackDoesNotConsumeTheChallenge() {
    var id = account();
    issue(id);
    assertThrows(
        IllegalStateException.class,
        () ->
            transaction()
                .executeWithoutResult(
                    s -> {
                      var state = recoveries.findByAccountIdForUpdate(id).orElseThrow();
                      recoveries.save(id, state.attempt(true, now.plusSeconds(1)).challenge());
                      throw new IllegalStateException("Simulated password mutation failure");
                    }));
    assertNull(recoveries.findByAccountId(id).orElseThrow().consumedAt());
  }

  @Test
  void concurrentConsumersCannotBothSucceed() throws Exception {
    var id = account();
    issue(id);
    var results =
        parallel(
            () ->
                transaction()
                    .execute(
                        s -> {
                          var state = recoveries.findByAccountIdForUpdate(id).orElseThrow();
                          var attempt = state.attempt(true, now.plusSeconds(1));
                          recoveries.save(id, attempt.challenge());
                          return attempt.outcome();
                        }));
    assertEquals(1, Collections.frequency(results, PasswordRecoveryChallenge.Outcome.ACCEPTED));
    assertEquals(1, Collections.frequency(results, PasswordRecoveryChallenge.Outcome.ALREADY_USED));
    assertEquals(now.plusSeconds(1), recoveries.findByAccountId(id).orElseThrow().consumedAt());
  }

  @Test
  void accountLockAlsoSerializesFirstIssuanceWithoutExistingRecoveryRow() throws Exception {
    var id = account();
    var results =
        parallel(
            () ->
                transaction()
                    .execute(
                        s -> {
                          if (recoveries.findByAccountIdForUpdate(id).isPresent()) return false;
                          recoveries.save(id, challenge());
                          return true;
                        }));
    assertEquals(1, Collections.frequency(results, true));
    assertEquals(1, Collections.frequency(results, false));
  }

  @Test
  void mutationsRequireAnOuterTransaction() {
    var id = account();
    assertThrows(
        IllegalTransactionStateException.class, () -> recoveries.findByAccountIdForUpdate(id));
    assertThrows(IllegalTransactionStateException.class, () -> recoveries.save(id, challenge()));
    assertTrue(recoveries.findByAccountId(id).isEmpty());
  }

  private <T> List<T> parallel(Callable<T> operation) throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<T> gated =
          () -> {
            start.await();
            return operation.call();
          };
      var first = executor.submit(gated);
      var second = executor.submit(gated);
      start.countDown();
      return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
    }
  }
}
