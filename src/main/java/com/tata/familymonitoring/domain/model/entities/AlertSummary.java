package com.tata.familymonitoring.domain.model.entities;

import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** Follow-up state of an alert received from Omission & Escalation. */
@Entity
public class AlertSummary {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String intakeId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private Instant scheduledAt;

  @Column(nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertStatus status;

  @Column(nullable = false)
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
}
