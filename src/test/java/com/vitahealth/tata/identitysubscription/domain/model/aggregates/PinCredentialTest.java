package com.vitahealth.tata.identitysubscription.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PinCredentialTest {
  @Test
  void invalidPolicyOrCounterDoesNotCorruptCredentialState() {
    var now = Instant.parse("2026-10-05T12:00:00Z");
    var credential = PinCredential.register("adult", "hash");
    assertThrows(NullPointerException.class, () -> credential.registerFailure(now, null));
    assertEquals(0, credential.failedAttempts());
    assertNull(credential.lockedUntil());
    assertThrows(
        IllegalArgumentException.class,
        () -> PinCredential.rehydrate("id", "adult", "hash", -1, null));
    var overflow = PinCredential.rehydrate("id", "adult", "hash", Integer.MAX_VALUE, null);
    assertThrows(
        ArithmeticException.class, () -> overflow.registerFailure(now, PinPolicy.defaultPolicy()));
    assertEquals(Integer.MAX_VALUE, overflow.failedAttempts());
    assertNull(overflow.lockedUntil());
  }

  @Test
  void locksAfterConfiguredFailedAttemptsAndResetsAfterSuccess() {
    var now = Instant.parse("2026-10-05T12:00:00Z");
    var policy = new PinPolicy(4, Duration.ofMinutes(15));
    var credential = PinCredential.register("older-adult-1", "hash");

    for (int i = 0; i < 4; i++) {
      credential.registerFailure(now, policy);
    }

    assertTrue(credential.isLockedAt(now.plusSeconds(1)));
    assertEquals(4, credential.failedAttempts());

    credential.registerSuccess();

    assertFalse(credential.isLockedAt(now.plusSeconds(1)));
    assertEquals(0, credential.failedAttempts());
  }

  @Test
  void lockExpiresAfterPolicyDuration() {
    var now = Instant.parse("2026-10-05T12:00:00Z");
    var policy = new PinPolicy(1, Duration.ofMinutes(15));
    var credential = PinCredential.register("older-adult-1", "hash");

    credential.registerFailure(now, policy);

    assertTrue(credential.isLockedAt(now.plusSeconds(30)));
    assertFalse(credential.isLockedAt(now.plus(Duration.ofMinutes(15))));
  }
}
