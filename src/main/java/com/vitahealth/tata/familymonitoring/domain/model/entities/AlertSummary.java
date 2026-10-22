package com.vitahealth.tata.familymonitoring.domain.model.entities;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.exceptions.InvalidAlertTransitionException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;
import java.util.Objects;

/** Follow-up state of an alert received from Omission & Escalation. */
public class AlertSummary {

  private Long id;

  private final String intakeId;

  private final String medicationName;

  private final Instant scheduledAt;

  private final String reason;

  private AlertStatus status;

  private final Instant openedAt;

  private Instant closedAt;

  /** Creates an open alert from original omission evidence. */
  public AlertSummary(
      String intakeId,
      String medicationName,
      Instant scheduledAt,
      String reason,
      Instant openedAt) {
    requireText(intakeId, "intakeId");
    requireText(medicationName, "medicationName");
    requireText(reason, "reason");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
    Objects.requireNonNull(openedAt, "openedAt");
    if (openedAt.isBefore(scheduledAt))
      throw new IllegalArgumentException("alert opening must not precede the omitted schedule");
    this.intakeId = intakeId;
    this.medicationName = medicationName;
    this.scheduledAt = scheduledAt;
    this.reason = reason;
    this.openedAt = openedAt;
    this.status = AlertStatus.OPEN;
  }

  /** Records attention once while preserving the original omission evidence. */
  public void markAttended() {
    if (status != AlertStatus.OPEN) {
      throw new InvalidAlertTransitionException("Only an open alert can be marked as attended");
    }
    this.status = AlertStatus.ATTENDED;
  }

  /** Closes the alert. It stays in the history. */
  public void close(Instant now) {
    if (status == AlertStatus.CLOSED) {
      throw new InvalidAlertTransitionException("The alert is already closed");
    }
    Objects.requireNonNull(now, "closedAt");
    if (now.isBefore(openedAt))
      throw new IllegalArgumentException("alert closure must not precede its opening");
    this.status = AlertStatus.CLOSED;
    this.closedAt = now;
  }

  /** Reports whether explicit closure is still pending, including recorded attention. */
  public boolean isPending() {
    return status != AlertStatus.CLOSED;
  }

  /** Returns the stored identity, absent until first persistence. */
  public Long getId() {
    return id;
  }

  /** Returns the original omitted intake reference. */
  public String getIntakeId() {
    return intakeId;
  }

  /** Returns the medication name recorded in the source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /** Returns the original omitted schedule instant. */
  public Instant getScheduledAt() {
    return scheduledAt;
  }

  /** Returns the original omission reason. */
  public String getReason() {
    return reason;
  }

  /** Returns the current caregiver follow-up state. */
  public AlertStatus getStatus() {
    return status;
  }

  /** Returns the source omission occurrence instant. */
  public Instant getOpenedAt() {
    return openedAt;
  }

  /** Returns the closure instant, absent before closure. */
  public Instant getClosedAt() {
    return closedAt;
  }

  /** Returns detached evidence so callers cannot mutate an aggregate-owned alert through a read. */
  public AlertSummary snapshot() {
    var copy = new AlertSummary(intakeId, medicationName, scheduledAt, reason, openedAt);
    copy.id = id;
    copy.status = status;
    copy.closedAt = closedAt;
    return copy;
  }

  /** Restores validated stored evidence, preserving original text and timestamps. */
  public static AlertSummary rehydrate(
      Long id,
      String intakeId,
      String medicationName,
      Instant scheduledAt,
      String reason,
      AlertStatus status,
      Instant openedAt,
      Instant closedAt) {
    if (id == null || id <= 0)
      throw new IllegalArgumentException("stored alert identity must be positive");
    Objects.requireNonNull(status, "status");
    if ((status == AlertStatus.CLOSED) != (closedAt != null))
      throw new IllegalArgumentException("only a closed alert requires a closure time");
    var restored = new AlertSummary(intakeId, medicationName, scheduledAt, reason, openedAt);
    if (closedAt != null && closedAt.isBefore(openedAt))
      throw new IllegalArgumentException("alert closure must not precede its opening");
    restored.id = id;
    restored.status = status;
    restored.closedAt = closedAt;
    return restored;
  }
}
