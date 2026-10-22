package com.vitahealth.tata.identitysubscription.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns the encoded adult PIN and persisted failed-attempt lock state. */
public final class PinCredential {
  private final String id;
  private final String olderAdultId;
  private final String pinHash;
  private int failedAttempts;
  private Instant lockedUntil;

  private PinCredential(
      String id, String olderAdultId, String pinHash, int failedAttempts, Instant lockedUntil) {
    this.id = Objects.requireNonNull(id);
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.pinHash = requireText(pinHash, "pinHash");
    if (failedAttempts < 0) {
      throw new IllegalArgumentException("failedAttempts cannot be negative");
    }
    this.failedAttempts = failedAttempts;
    this.lockedUntil = lockedUntil;
  }

  /** Creates an unlocked credential containing only the encoded PIN. */
  public static PinCredential register(String olderAdultId, String pinHash) {
    return new PinCredential(UUID.randomUUID().toString(), olderAdultId, pinHash, 0, null);
  }

  /** Restores persisted attempt history without silently correcting corrupted counters. */
  public static PinCredential rehydrate(
      String id, String olderAdultId, String pinHash, int failedAttempts, Instant lockedUntil) {
    return new PinCredential(id, olderAdultId, pinHash, failedAttempts, lockedUntil);
  }

  /** Treats the lock expiration as an exclusive bound. */
  public boolean isLockedAt(Instant now) {
    Objects.requireNonNull(now, "now");
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  /** Records an unlocked failure atomically and applies the configured temporary lock. */
  public void registerFailure(Instant now, PinPolicy policy) {
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(policy, "policy");
    if (isLockedAt(now)) return;
    int replacementAttempts = Math.incrementExact(failedAttempts);
    var replacementLock =
        replacementAttempts >= policy.maxFailedAttempts()
            ? now.plus(policy.lockDuration())
            : lockedUntil;
    failedAttempts = replacementAttempts;
    lockedUntil = replacementLock;
  }

  /** Resets attempt history and removes the lock after successful authentication. */
  public void registerSuccess() {
    failedAttempts = 0;
    lockedUntil = null;
  }

  /** Returns the stable credential identity. */
  public String id() {
    return id;
  }

  /** Returns the adult owning this credential. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the encoded PIN for matching and persistence only. */
  public String pinHash() {
    return pinHash;
  }

  /** Returns persisted consecutive failure history since the last successful authentication. */
  public int failedAttempts() {
    return failedAttempts;
  }

  /** Returns the lock expiration or null when there is no lock. */
  public Instant lockedUntil() {
    return lockedUntil;
  }
}
