package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicateAccountEmailException;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Verifies that only the email uniqueness constraint becomes the expected domain conflict. */
@SpringBootTest
@ActiveProfiles("test")
class AccountEmailConflictIntegrationTest {
  @Autowired AccountRepository accounts;
  @Autowired AccountCommandService commands;

  @Test
  void concurrentRegistrationsReturnOneWinnerAndOneExpectedConflict() throws Exception {
    String email = UUID.randomUUID() + "@example.com";
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                return commands.register(
                    new RegisterFamilyAccountCommand("Rosa", email, "password-123"));
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                return commands.register(
                    new RegisterFamilyAccountCommand("Other", email, "password-123"));
              });
      start.countDown();
      var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter(Result::isSuccess).count());
      var failure = results.stream().filter(Result::isFailure).findFirst().orElseThrow();
      var error = ((Result.Failure<?, ?>) failure).error();
      assertEquals("DUPLICATE_EMAIL", ((ApplicationError) error).code());
    }
  }

  @Test
  void duplicateInsertIsClassifiedAfterRollbackWithoutReplacingTheWinner() {
    String email = UUID.randomUUID() + "@example.com";
    var winner = account(email, "Rosa");
    accounts.save(winner);
    assertThrows(
        DuplicateAccountEmailException.class, () -> accounts.save(account(email, "Other")));
    assertEquals(winner.id(), accounts.findByEmail(email).orElseThrow().id());
  }

  @Test
  void unrelatedIntegrityFailureIsNotConvertedIntoDuplicateEmail() {
    assertThrows(
        DataIntegrityViolationException.class,
        () -> accounts.save(account(UUID.randomUUID() + "@example.com", "x".repeat(121))));
  }

  private Account account(String email, String name) {
    return Account.register(
        name, new EmailAddress(email), "hash", "challenge", Instant.now(), Duration.ofMinutes(15));
  }
}
