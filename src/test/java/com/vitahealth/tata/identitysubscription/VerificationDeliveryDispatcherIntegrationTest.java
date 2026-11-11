package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationEmailProvider;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.vitahealth.tata.identitysubscription.infrastructure.email.queue.VerificationDeliveryDispatcher;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.VerificationDeliveryPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies committed registration intentions reach the provider outside database transactions. */
@SpringBootTest
@ActiveProfiles("test")
class VerificationDeliveryDispatcherIntegrationTest {
  @Autowired AccountCommandService accounts;
  @Autowired VerificationDeliveryDispatcher dispatcher;
  @Autowired VerificationDeliveryJpaRepository deliveries;
  @Autowired PlatformTransactionManager transactions;
  @MockitoBean VerificationEmailProvider provider;

  @Test
  void deliversRegistrationOutsideTransactionAndErasesSecret() {
    String email = UUID.randomUUID() + "@example.com";
    assertTrue(
        accounts
            .register(new RegisterFamilyAccountCommand("Rosa", email, "password-123"))
            .toOptional()
            .isPresent());
    var row =
        deliveries.findAll().stream()
            .filter(value -> value.email().equals(email))
            .findFirst()
            .orElseThrow();
    when(provider.send(any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return "provider-accepted";
            });
    new TransactionTemplate(transactions).executeWithoutResult(status -> dispatcher.dispatch());
    verify(provider).send(any());
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    deliveries.deleteById(row.id());
  }

  @Test
  void cancelsExpiredOrMissingAccountAndErasesSecret() {
    String id = UUID.randomUUID().toString();
    deliveries.save(
        new VerificationDeliveryPersistenceEntity(
            id,
            "missing",
            "expired@example.com",
            "hash",
            "ciphertext",
            Instant.now().minusSeconds(30),
            Instant.now().minusSeconds(60)));
    dispatcher.dispatch();
    assertNull(deliveries.findById(id).orElseThrow().encryptedCode());
    verifyNoInteractions(provider);
    deliveries.deleteById(id);
  }

  @Test
  void renewalCancelsPreviousPayloadAndSendsOnlyCurrentChallenge() {
    String email = UUID.randomUUID() + "@example.com";
    assertTrue(
        accounts
            .register(new RegisterFamilyAccountCommand("Rosa", email, "password-123"))
            .toOptional()
            .isPresent());
    var previous =
        deliveries.findAll().stream()
            .filter(value -> value.email().equals(email))
            .findFirst()
            .orElseThrow();
    assertTrue(
        accounts
            .requestNewVerification(new RequestNewVerificationCommand(email))
            .toOptional()
            .isPresent());
    when(provider.send(any())).thenReturn("accepted-current");
    dispatcher.dispatch();
    verify(provider).send(any());
    assertNull(deliveries.findById(previous.id()).orElseThrow().encryptedCode());
    var rows = deliveries.findAll().stream().filter(value -> value.email().equals(email)).toList();
    assertTrue(rows.stream().allMatch(value -> value.encryptedCode() == null));
    deliveries.deleteAll(rows);
  }

  @Test
  void concurrentWorkerDoesNotSendReservedDelivery() throws Exception {
    String email = UUID.randomUUID() + "@example.com";
    assertTrue(
        accounts
            .register(new RegisterFamilyAccountCommand("Rosa", email, "password-123"))
            .toOptional()
            .isPresent());
    var row =
        deliveries.findAll().stream()
            .filter(value -> value.email().equals(email))
            .findFirst()
            .orElseThrow();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(provider.send(any()))
        .thenAnswer(
            invocation -> {
              var request =
                  invocation.getArgument(
                      0,
                      com.vitahealth.tata.identitysubscription.application.internal.outboundservices
                          .VerificationDeliveryRequest.class);
              if (!row.id().equals(request.deliveryId())) {
                return "accepted-other-delivery";
              }
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              entered.countDown();
              assertTrue(release.await(45, TimeUnit.SECONDS));
              return "accepted-concurrent";
            });
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first = workers.submit(() -> dispatcher.dispatch());
      try {
        assertTrue(entered.await(20, TimeUnit.SECONDS));
        var secondStarted = new CountDownLatch(1);
        var second =
            workers.submit(
                () -> {
                  secondStarted.countDown();
                  dispatcher.dispatch();
                });
        // A concurrent database reader may wait for the first sender to complete.
        // Release the provider before joining either worker, then prove one delivery.
        assertTrue(secondStarted.await(20, TimeUnit.SECONDS));
        release.countDown();
        first.get(45, TimeUnit.SECONDS);
        second.get(45, TimeUnit.SECONDS);
        verify(provider)
            .send(org.mockito.ArgumentMatchers.argThat(request -> row.id().equals(request.deliveryId())));
      } finally {
        release.countDown();
      }
    }
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    deliveries.deleteById(row.id());
  }
}
