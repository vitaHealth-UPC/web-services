package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.VerificationDeliveryPersistenceEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies worker ownership, crash recovery and terminal secret erasure. */
class VerificationDeliveryReservationTest {
  @Test
  void recoversExpiredLeaseAndRejectsStaleWorker() {
    Instant now = Instant.parse("2026-10-12T12:00:00Z");
    var row =
        new VerificationDeliveryPersistenceEntity(
            "id", "account", "email@example.com", "hash", "ciphertext", now.plusSeconds(900), now);
    assertTrue(row.claim("worker-1", now));
    assertFalse(row.claim("worker-2", now.plusSeconds(59)));
    assertTrue(row.isOwnedBy("worker-1"));
    assertTrue(row.claim("worker-2", now.plusSeconds(60)));
    assertFalse(row.isOwnedBy("worker-1"));
    row.retry(now.plusSeconds(60));
    assertFalse(row.isOwnedBy("worker-2"));
    assertFalse(row.claim("worker-3", now.plusSeconds(65)));
    assertTrue(row.claim("worker-3", now.plusSeconds(70)));
    row.complete("accepted");
    assertNull(row.encryptedCode());
    assertFalse(row.claim("worker-4", now.plusSeconds(1000)));
  }
}
