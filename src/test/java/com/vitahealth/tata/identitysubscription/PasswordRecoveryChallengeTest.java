package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PasswordRecoveryChallenge;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PasswordRecoveryChallenge.Outcome;
import java.time.*;
import org.junit.jupiter.api.Test;

class PasswordRecoveryChallengeTest {
  private final Instant now = Instant.parse("2026-10-26T12:00:00Z");

  private PasswordRecoveryChallenge challenge() {
    return PasswordRecoveryChallenge.issue("encoded-private-code", now, Duration.ofMinutes(10));
  }

  @Test
  void successfulCodeIsConsumedExactlyOnce() {
    var source = challenge();
    var accepted = source.attempt(true, now.plusSeconds(1));
    assertEquals(Outcome.ACCEPTED, accepted.outcome());
    assertEquals(now.plusSeconds(1), accepted.challenge().consumedAt());
    assertNull(source.consumedAt());
    assertEquals(
        Outcome.ALREADY_USED, accepted.challenge().attempt(true, now.plusSeconds(2)).outcome());
    assertEquals(
        Outcome.ALREADY_USED, accepted.challenge().attempt(false, now.plusSeconds(2)).outcome());
  }

  @Test
  void expirationIsExclusiveEvenWithCorrectCode() {
    assertEquals(Outcome.ACCEPTED, challenge().attempt(true, now.plusSeconds(599)).outcome());
    assertEquals(Outcome.EXPIRED, challenge().attempt(true, now.plusSeconds(600)).outcome());
  }

  @Test
  void failedAttemptsPersistAndCorrectCodeCannotBypassLock() {
    var current = challenge();
    for (int i = 1; i <= 5; i++) {
      var attempt = current.attempt(false, now.plusSeconds(i));
      assertEquals(i == 5 ? Outcome.LOCKED : Outcome.INVALID_CODE, attempt.outcome());
      current = attempt.challenge();
      assertEquals(i, current.failedAttempts());
      assertNull(current.consumedAt());
    }
    assertEquals(Outcome.LOCKED, current.attempt(true, now.plusSeconds(6)).outcome());
  }

  @Test
  void invalidHydratedStateAndDiagnosticLeaksAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PasswordRecoveryChallenge.issue("hash", now, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PasswordRecoveryChallenge("hash", now, now.plusSeconds(10), 6, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PasswordRecoveryChallenge(
                "hash", now, now.plusSeconds(10), 0, now.plusSeconds(10)));
    assertThrows(
        IllegalArgumentException.class, () -> challenge().attempt(true, now.minusSeconds(1)));
    assertFalse(challenge().toString().contains("encoded-private-code"));
    assertFalse(challenge().attempt(true, now).toString().contains("encoded-private-code"));
  }
}
