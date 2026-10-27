package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.*;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.*;
import com.vitahealth.tata.identitysubscription.infrastructure.email.queue.VerificationDeliveryDispatcher;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

@SpringBootTest
@ActiveProfiles("test")
class PasswordRecoveryDeliveryIntegrationTest {
  @Autowired AccountRepository accounts;
  @Autowired PasswordRecoveryRepository recoveries;
  @Autowired QueuePasswordRecoveryDeliveryPort queue;
  @Autowired VerificationPayloadCipher cipher;
  @Autowired VerificationDeliveryDispatcher dispatcher;
  @Autowired VerificationDeliveryJpaRepository deliveries;
  @Autowired AccountJpaRepository accountRows;
  @Autowired PasswordRecoveryJpaRepository recoveryRows;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;
  @MockitoBean VerificationEmailProvider provider;
  private final List<String> created = new ArrayList<>();

  private Account account() {
    var now = clock.instant();
    var account =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "activation",
            now,
            Duration.ofMinutes(15));
    account.completeVerification();
    account = accounts.save(account);
    created.add(account.id());
    return account;
  }

  private PasswordRecoveryChallenge challenge() {
    return PasswordRecoveryChallenge.issue(
        "recovery-hash", clock.instant().minusSeconds(5), Duration.ofMinutes(10));
  }

  private void issue(Account account, PasswordRecoveryChallenge challenge) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              recoveries.save(account.id(), challenge);
              queue.queueRecovery(
                  account.id(),
                  account.email().value(),
                  "012345",
                  challenge.codeHash(),
                  challenge.expiresAt());
            });
  }

  @AfterEach
  void removeOwnFixtures() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              deliveries.findAll().stream()
                  .filter(row -> created.contains(row.accountId()))
                  .forEach(deliveries::delete);
              for (var id : created) {
                recoveryRows.findById(id).ifPresent(recoveryRows::delete);
                accountRows.deleteById(id);
              }
            });
  }

  @Test
  void encryptsRecoveryAndDispatchesItsPurposeOutsideTheDatabaseTransaction() {
    var account = account();
    issue(account, challenge());
    var row =
        deliveries.findAll().stream()
            .filter(value -> value.accountId().equals(account.id()))
            .findFirst()
            .orElseThrow();
    assertEquals(CredentialEmailPurpose.PASSWORD_RECOVERY, row.purpose());
    assertFalse(row.encryptedCode().contains("012345"));
    assertEquals("012345", cipher.decrypt(row.id(), row.encryptedCode()));
    when(provider.send(any()))
        .thenAnswer(
            call -> {
              var request = call.getArgument(0, VerificationDeliveryRequest.class);
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              if (request.deliveryId().equals(row.id())) {
                assertEquals(CredentialEmailPurpose.PASSWORD_RECOVERY, request.purpose());
                assertEquals(account.email().value(), request.email());
                assertEquals("012345", request.code());
              }
              return "accepted";
            });
    dispatcher.dispatch();
    verify(provider).send(argThat(request -> request.deliveryId().equals(row.id())));
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
  }

  @Test
  void doesNotSendConsumedSupersededOrLockedRecoveryCodes() {
    for (var state : List.of("consumed", "superseded", "locked")) {
      var account = account();
      var original = challenge();
      issue(account, original);
      var replacement = original;
      if (state.equals("consumed"))
        replacement = original.attempt(true, clock.instant()).challenge();
      if (state.equals("superseded"))
        replacement =
            PasswordRecoveryChallenge.issue("new-hash", clock.instant(), Duration.ofMinutes(10));
      if (state.equals("locked"))
        for (int i = 0; i < 5; i++)
          replacement = replacement.attempt(false, clock.instant()).challenge();
      var latest = replacement;
      new TransactionTemplate(manager)
          .executeWithoutResult(s -> recoveries.save(account.id(), latest));
      dispatcher.dispatch();
      verify(provider, never())
          .send(argThat(request -> request.email().equals(account.email().value())));
      assertNull(
          deliveries.findAll().stream()
              .filter(value -> value.accountId().equals(account.id()))
              .findFirst()
              .orElseThrow()
              .encryptedCode());
    }
  }

  @Test
  void rollbackLeavesNeitherChallengeNorDeliveryIntention() {
    var account = account();
    var challenge = challenge();
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(manager)
                .executeWithoutResult(
                    s -> {
                      recoveries.save(account.id(), challenge);
                      queue.queueRecovery(
                          account.id(),
                          account.email().value(),
                          "012345",
                          challenge.codeHash(),
                          challenge.expiresAt());
                      throw new IllegalStateException("Simulated issuance failure");
                    }));
    assertTrue(recoveries.findByAccountId(account.id()).isEmpty());
    assertTrue(
        deliveries.findAll().stream().noneMatch(value -> value.accountId().equals(account.id())));
  }
}
