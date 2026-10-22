package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

import java.time.Duration;

/** Defines the persisted failed-attempt threshold and temporary PIN lock duration. */
public record PinPolicy(int maxFailedAttempts, Duration lockDuration) {
  /** Requires a positive threshold and duration without changing failed-attempt history. */
  public PinPolicy {
    if (maxFailedAttempts < 1)
      throw new IllegalArgumentException("maxFailedAttempts must be positive");
    if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
      throw new IllegalArgumentException("lockDuration must be positive");
    }
  }

  /** Returns the current product policy: four failures lock access for fifteen minutes. */
  public static PinPolicy defaultPolicy() {
    return new PinPolicy(4, Duration.ofMinutes(15));
  }
}
