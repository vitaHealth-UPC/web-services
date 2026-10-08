package com.tata.familymonitoring.domain.model.entities;

import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;

/** Follow-up state of an alert received from Omission & Escalation. */

public class AlertSummary {


  private Long id;

  private String intakeId;

  private String medicationName;

  private Instant scheduledAt;

  private String reason;


  private AlertStatus status;

  private Instant openedAt;

  private Instant closedAt;

  protected AlertSummary() {
  }

  public AlertSummary(
      String intakeId,
      String medicationName,
      Instant scheduledAt,
      String reason,
      Instant openedAt) {
    this.intakeId = intakeId;
    this.medicationName = medicationName;
    this.scheduledAt = scheduledAt;
    this.reason = reason;
    this.openedAt = openedAt;
    this.status = AlertStatus.OPEN;
  }

  public void markAttended() {
    if (status != AlertStatus.OPEN) {
      throw new IllegalStateException("Only an open alert can be marked as attended");
    }
    this.status = AlertStatus.ATTENDED;
  }

  /** Closes the alert. It stays in the history. */
  public void close(Instant now) {
    if (status == AlertStatus.CLOSED) {
      throw new IllegalStateException("The alert is already closed");
    }
    this.status = AlertStatus.CLOSED;
    this.closedAt = now;
  }

  public boolean isOpen() {
    return status == AlertStatus.OPEN;
  }

  public Long getId() {
    return id;
  }

  public String getIntakeId() {
    return intakeId;
  }

  public String getMedicationName() {
    return medicationName;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public String getReason() {
    return reason;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  /** Restores persisted state without replaying business actions. */
  public static AlertSummary rehydrate(Long id, String intakeId, String medicationName, Instant scheduledAt, String reason, AlertStatus status, Instant openedAt, Instant closedAt) {
    var restored = new AlertSummary();
    restored.id = id;
    restored.intakeId = intakeId;
    restored.medicationName = medicationName;
    restored.scheduledAt = scheduledAt;
    restored.reason = reason;
    restored.status = status;
    restored.openedAt = openedAt;
    restored.closedAt = closedAt;
    return restored;
  }
}
