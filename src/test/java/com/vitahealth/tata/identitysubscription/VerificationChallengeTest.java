package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationChallenge;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Checks atomic encoded challenge evidence and persisted reconstruction boundaries. */
class VerificationChallengeTest {
  private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

  @Test
  void expirationIsExclusiveAndDiagnosticsOmitTheHash() {
    var challenge = VerificationChallenge.issue("encoded-secret", NOW, Duration.ofMinutes(15));
    assertThat(challenge.isValidAt(NOW.plusSeconds(899))).isTrue();
    assertThat(challenge.isValidAt(NOW.plusSeconds(900))).isFalse();
    assertThat(challenge.toString()).doesNotContain("encoded-secret");
    assertThat(new VerificationChallenge("hash", NOW.minusSeconds(1)).isValidAt(NOW)).isFalse();
  }

  @Test
  void reconstructionRejectsHalfOfAChallenge() {
    assertThatIllegalArgumentException().isThrownBy(() -> restore("hash", null));
    assertThatIllegalArgumentException().isThrownBy(() -> restore(null, NOW));
    assertThatIllegalArgumentException().isThrownBy(() -> restore(" ", NOW));
    assertThat(restore(null, null).canCompleteVerification(NOW)).isFalse();
  }

  private Account restore(String hash, Instant expiry) {
    return Account.rehydrate(
        "account",
        "Rosa",
        new EmailAddress("rosa@example.com"),
        "password-hash",
        AccountStatus.PENDING_VERIFICATION,
        hash,
        expiry);
  }
}
