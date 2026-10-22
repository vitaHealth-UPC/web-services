package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.QueueVerificationDeliveryPort;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies durable encrypted intentions participate in the caller's transaction. */
@SpringBootTest
@ActiveProfiles("test")
class VerificationDeliveryQueueIntegrationTest {
  @Autowired QueueVerificationDeliveryPort queue;
  @Autowired VerificationDeliveryJpaRepository repository;
  @Autowired VerificationPayloadCipher cipher;
  @Autowired PlatformTransactionManager transactions;

  @Test
  void commitsEncryptedPayloadAndRollsBackFailedCommand() {
    long before = repository.count();
    var transaction = new TransactionTemplate(transactions);
    transaction.executeWithoutResult(
        status ->
            queue.queue(
                "account-test",
                "queue@example.com",
                "012345",
                "challenge-hash",
                Instant.now().plusSeconds(900)));
    assertEquals(before + 1, repository.count());
    var row =
        repository.findAll().stream()
            .filter(value -> value.accountId().equals("account-test"))
            .findFirst()
            .orElseThrow();
    assertFalse(row.encryptedCode().contains("012345"));
    assertEquals("012345", cipher.decrypt(row.id(), row.encryptedCode()));
    assertThrows(
        IllegalStateException.class,
        () ->
            transaction.executeWithoutResult(
                status -> {
                  queue.queue(
                      "account-rollback",
                      "rollback@example.com",
                      "654321",
                      "other-hash",
                      Instant.now().plusSeconds(900));
                  throw new IllegalStateException("command failed");
                }));
    assertEquals(before + 1, repository.count());
    repository.deleteById(row.id());
  }
}
