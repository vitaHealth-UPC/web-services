package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Recovery-only encoded evidence. A successful attempt must commit with the password change. */
public record PasswordRecoveryChallenge(
    String codeHash, Instant issuedAt, Instant expiresAt, int failedAttempts, Instant consumedAt) {
  public static final int MAX_ATTEMPTS = 5;

  public PasswordRecoveryChallenge {
    codeHash = requireText(codeHash, "codeHash");
    Objects.requireNonNull(issuedAt, "issuedAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (!expiresAt.isAfter(issuedAt) || failedAttempts < 0 || failedAttempts > MAX_ATTEMPTS)
      throw new IllegalArgumentException("Invalid recovery challenge state");
    if (consumedAt != null
        && (consumedAt.isBefore(issuedAt)
            || !consumedAt.isBefore(expiresAt)
            || failedAttempts == MAX_ATTEMPTS))
      throw new IllegalArgumentException("Invalid recovery consumption evidence");
  }

  public static PasswordRecoveryChallenge issue(String hash, Instant now, Duration lifetime) {
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(lifetime, "lifetime");
    if (lifetime.isZero() || lifetime.isNegative())
      throw new IllegalArgumentException("Recovery lifetime must be positive");
    return new PasswordRecoveryChallenge(hash, now, now.plus(lifetime), 0, null);
  }

  /** The application verifies the encoded code and persists this result under a mutation lock. */
  public Attempt attempt(boolean matches, Instant now) {
    Objects.requireNonNull(now, "now");
    if (now.isBefore(issuedAt))
      throw new IllegalArgumentException("Recovery time precedes issuance");
    if (consumedAt != null) return new Attempt(Outcome.ALREADY_USED, this);
    if (!now.isBefore(expiresAt)) return new Attempt(Outcome.EXPIRED, this);
    if (failedAttempts == MAX_ATTEMPTS) return new Attempt(Outcome.LOCKED, this);
    if (matches)
      return new Attempt(
          Outcome.ACCEPTED,
          new PasswordRecoveryChallenge(codeHash, issuedAt, expiresAt, failedAttempts, now));
    var next =
        new PasswordRecoveryChallenge(codeHash, issuedAt, expiresAt, failedAttempts + 1, null);
    return new Attempt(
        next.failedAttempts == MAX_ATTEMPTS ? Outcome.LOCKED : Outcome.INVALID_CODE, next);
  }

  public enum Outcome {
    ACCEPTED,
    INVALID_CODE,
    EXPIRED,
    LOCKED,
    ALREADY_USED
  }

  public record Attempt(Outcome outcome, PasswordRecoveryChallenge challenge) {}

  @Override
  public String toString() {
    return "PasswordRecoveryChallenge[codeHash=REDACTED, failedAttempts="
        + failedAttempts
        + ", consumed="
        + (consumedAt != null)
        + "]";
  }
}
