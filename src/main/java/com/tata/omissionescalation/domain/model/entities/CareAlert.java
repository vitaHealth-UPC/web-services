package com.tata.omissionescalation.domain.model.entities;

import com.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import java.time.Instant;

/** Alert generated for the caregiver from an omission, with the result of its delivery. */

public class CareAlert {


  private Long id;


  private AlertStatus status;

  private Instant generatedAt;

  private Instant sentAt;

  private String failureReason;

  protected CareAlert() {
  }

  public CareAlert(Instant generatedAt) {
    this.status = AlertStatus.GENERATED;
    this.generatedAt = generatedAt;
  }

  public void markSent(Instant sentAt) {
    this.status = AlertStatus.SENT;
    this.sentAt = sentAt;
    this.failureReason = null;
  }

  public void markFailed(String failureReason) {
    this.status = AlertStatus.FAILED;
    this.failureReason = failureReason;
  }

  public Long getId() {
    return id;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getFailureReason() {
    return failureReason;
  }

  /** Restores persisted state without replaying business actions. */
  public static CareAlert rehydrate(Long id, AlertStatus status, Instant generatedAt, Instant sentAt, String failureReason) {
    var restored = new CareAlert();
    restored.id = id;
    restored.status = status;
    restored.generatedAt = generatedAt;
    restored.sentAt = sentAt;
    restored.failureReason = failureReason;
    return restored;
  }
}
