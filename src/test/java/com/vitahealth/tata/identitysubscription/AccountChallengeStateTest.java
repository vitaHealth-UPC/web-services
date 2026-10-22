package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies rejected challenge replacements cannot partially mutate the account aggregate. */
class AccountChallengeStateTest {
  @Test
  void invalidLifetimeLeavesPreviousChallengeIntact() {
    Instant now = Instant.parse("2026-10-12T12:00:00Z");
    var account =
        Account.register(
            "Rosa",
            new EmailAddress("rosa@example.com"),
            "hash",
            "old-challenge",
            now,
            Duration.ofMinutes(15));
    for (Duration lifetime : new Duration[] {Duration.ZERO, Duration.ofMinutes(-1)}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> account.renewVerificationCode("new-challenge", now, lifetime));
      assertEquals("old-challenge", account.verificationCodeHash());
      assertEquals(now.plusSeconds(900), account.verificationExpiresAt());
    }
    assertFalse(account.canCompleteVerification(now.plusSeconds(900)));
  }
}
