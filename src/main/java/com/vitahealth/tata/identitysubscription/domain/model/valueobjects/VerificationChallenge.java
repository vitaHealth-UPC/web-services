package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Encoded verification evidence whose hash and expiration form one immutable domain value. */
public record VerificationChallenge(String codeHash, Instant expiresAt) {
  /** Requires both pieces of evidence; expired values remain valid for persisted reconstruction. */
  public VerificationChallenge {
    codeHash = requireText(codeHash, "verificationCodeHash");
    Objects.requireNonNull(expiresAt, "verificationExpiresAt");
  }

  /** Creates a challenge with a strictly positive lifetime without retaining the plaintext code. */
  public static VerificationChallenge issue(String codeHash, Instant now, Duration lifetime) {
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(lifetime, "verificationTtl");
    if (lifetime.isZero() || lifetime.isNegative()) {
      throw new IllegalArgumentException("Verification lifetime must be positive");
    }
    return new VerificationChallenge(codeHash, now.plus(lifetime));
  }

  /** Uses an exclusive expiration boundary for verification authorization. */
  public boolean isValidAt(Instant now) {
    return Objects.requireNonNull(now, "now").isBefore(expiresAt);
  }

  /** Prevents encoded credential evidence from appearing in diagnostic messages. */
  @Override
  public String toString() {
    return "VerificationChallenge[codeHash=REDACTED, expiresAt=" + expiresAt + "]";
  }
}
