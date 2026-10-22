package com.vitahealth.tata.carelink.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/** Immutable adult decision; pending consent has no recorded decision time. */
public final class Consent {
  private final boolean accepted;
  private final Instant recordedAt;

  private Consent(boolean accepted, Instant recordedAt) {
    if (accepted && recordedAt == null) {
      throw new IllegalArgumentException("granted consent requires recorded evidence");
    }
    this.accepted = accepted;
    this.recordedAt = recordedAt;
  }

  /** Creates an undecided consent snapshot. */
  public static Consent pending() {
    return new Consent(false, null);
  }

  /** Records either approval or rejection with its required decision time. */
  public static Consent record(boolean accepted, Instant now) {
    return new Consent(accepted, Objects.requireNonNull(now));
  }

  /** Restores evidence without allowing an approval with no recorded decision. */
  public static Consent rehydrate(boolean accepted, Instant recordedAt) {
    return new Consent(accepted, recordedAt);
  }

  /** Reports explicit approval backed by a recorded decision time. */
  public boolean isGranted() {
    return accepted && recordedAt != null;
  }

  /** Returns the stored adult approval decision. */
  public boolean accepted() {
    return accepted;
  }

  /** Returns decision time, or null for pending consent. */
  public Instant recordedAt() {
    return recordedAt;
  }
}
