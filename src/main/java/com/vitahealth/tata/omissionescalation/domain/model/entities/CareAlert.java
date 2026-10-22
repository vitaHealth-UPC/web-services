package com.vitahealth.tata.omissionescalation.domain.model.entities;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import java.time.Instant;

/** Immutable caregiver-alert evidence whose replacements are owned by the omission aggregate. */
public final class CareAlert {

  private final Long id;

  private final AlertStatus status;

  private final Instant generatedAt;

  private final Instant sentAt;

  private final String failureReason;

  private CareAlert(
      Long id, AlertStatus status, Instant generatedAt, Instant sentAt, String failureReason) {
    this.id = id;
    this.status = status;
    this.generatedAt = generatedAt;
    this.sentAt = sentAt;
    this.failureReason = failureReason;
  }

  /** Creates unsent evidence at the explicitly supplied generation time. */
  public CareAlert(Instant generatedAt) {
    this(null, AlertStatus.GENERATED, generatedAt, null, null);
    if (generatedAt == null) throw new IllegalArgumentException("generation time is required");
  }

  /** Returns accepted evidence without modifying this snapshot or replacing prior success. */
  public CareAlert withSuccessfulDelivery(Instant sentAt) {
    requireUnsent();
    if (sentAt == null || sentAt.isBefore(generatedAt))
      throw new IllegalArgumentException("send time must not precede generation");
    return new CareAlert(id, AlertStatus.SENT, generatedAt, sentAt, null);
  }

  /** Returns failed evidence while preserving this snapshot's identity and generation time. */
  public CareAlert withFailedDelivery(String failureReason) {
    requireUnsent();
    if (failureReason == null || failureReason.isBlank())
      throw new IllegalArgumentException("failure reason is required");
    return new CareAlert(id, AlertStatus.FAILED, generatedAt, null, failureReason);
  }

  private void requireUnsent() {
    if (status == AlertStatus.SENT) throw new IllegalStateException("sent alert is final");
  }

  /** Returns the stored alert identity, absent before its first save. */
  public Long getId() {
    return id;
  }

  /** Returns the current delivery evidence state. */
  public AlertStatus getStatus() {
    return status;
  }

  /** Returns the original generation time. */
  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /** Returns the successful send time, or null when unsent. */
  public Instant getSentAt() {
    return sentAt;
  }

  /** Returns the failure reason only while delivery is failed. */
  public String getFailureReason() {
    return failureReason;
  }

  /** Restores persisted state without replaying business actions. */
  public static CareAlert rehydrate(
      Long id, AlertStatus status, Instant generatedAt, Instant sentAt, String failureReason) {
    if (id == null || id < 1 || status == null || generatedAt == null)
      throw new IllegalArgumentException(
          "stored alert identity, status and generation are required");
    boolean valid =
        switch (status) {
          case GENERATED -> sentAt == null && failureReason == null;
          case FAILED -> sentAt == null && failureReason != null && !failureReason.isBlank();
          case SENT -> sentAt != null && !sentAt.isBefore(generatedAt) && failureReason == null;
        };
    if (!valid)
      throw new IllegalArgumentException("stored alert delivery evidence is inconsistent");
    return new CareAlert(id, status, generatedAt, sentAt, failureReason);
  }
}
