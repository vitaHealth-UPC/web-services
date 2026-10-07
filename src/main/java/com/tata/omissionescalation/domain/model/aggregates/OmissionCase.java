package com.tata.omissionescalation.domain.model.aggregates;

import com.tata.omissionescalation.domain.model.entities.CareAlert;
import com.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Life cycle of an unconfirmed intake: grace period, omission, caregiver alert and escalation.
 * The intake and the older adult are referenced by logical id only.
 */

public class OmissionCase {


  private Long id;

  private String intakeId;

  private String olderAdultId;

  private String medicationName;

  private Instant scheduledAt;


  private OmissionCaseStatus status;

  private GracePeriod gracePeriod;

  private Instant reinforcedReminderSentAt;

  private Instant omittedAt;

  private Instant closedAt;


  private List<CareAlert> alerts = new ArrayList<>();


  private List<EscalationRecord> escalations = new ArrayList<>();


  private Instant createdAt;

  private Instant updatedAt;

  protected OmissionCase() {
  }

  public OmissionCase(
      String intakeId,
      String olderAdultId,
      String medicationName,
      Instant scheduledAt,
      GracePeriod gracePeriod) {
    this.intakeId = intakeId;
    this.olderAdultId = olderAdultId;
    this.medicationName = medicationName;
    this.scheduledAt = scheduledAt;
    this.gracePeriod = gracePeriod;
    this.status = OmissionCaseStatus.PENDING;
  }

  public boolean isGraceExpired(Instant now) {
    return gracePeriod.isExpired(now);
  }

  public void markReminderSent(Instant now) {
    requireStatus(OmissionCaseStatus.PENDING);
    if (reinforcedReminderSentAt == null) {
      this.reinforcedReminderSentAt = now;
    }
  }

  /** The intake was confirmed while the grace period was still active. */
  public void resolve(Instant now) {
    requireStatus(OmissionCaseStatus.PENDING);
    if (!gracePeriod.isActive(now)) {
      throw new IllegalStateException("The grace period is no longer active");
    }
    this.status = OmissionCaseStatus.RESOLVED;
  }

  /** The grace period ended without confirmation. */
  public void markOmitted(Instant now) {
    requireStatus(OmissionCaseStatus.PENDING);
    if (!gracePeriod.isExpired(now)) {
      throw new IllegalStateException("The grace period has not expired yet");
    }
    this.status = OmissionCaseStatus.OMITTED;
    this.omittedAt = now;
  }

  /** Creates the caregiver alert once; repeated calls return the existing one. */
  public CareAlert addAlert(Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    if (!alerts.isEmpty()) {
      return alerts.getFirst();
    }
    CareAlert alert = new CareAlert(now);
    alerts.add(alert);
    return alert;
  }

  public EscalationRecord escalate(String reason, Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    EscalationLevel nextLevel = currentLevel().next();
    EscalationRecord record = new EscalationRecord(nextLevel, reason, now);
    escalations.add(record);
    this.status = OmissionCaseStatus.ESCALATED;
    return record;
  }

  public void close(Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    this.status = OmissionCaseStatus.CLOSED;
    this.closedAt = now;
  }

  public EscalationLevel currentLevel() {
    if (escalations.isEmpty()) {
      return EscalationLevel.NONE;
    }
    return escalations.getLast().getLevel();
  }

  /** Last moment something happened in the omitted flow: the last escalation or the omission. */
  public Instant lastActivityAt() {
    if (escalations.isEmpty()) {
      return omittedAt;
    }
    return escalations.getLast().getTriggeredAt();
  }

  private void requireStatus(OmissionCaseStatus... allowed) {
    for (OmissionCaseStatus candidate : allowed) {
      if (status == candidate) {
        return;
      }
    }
    throw new IllegalStateException("Operation not allowed while the case is " + status);
  }

  public Long getId() {
    return id;
  }

  public String getIntakeId() {
    return intakeId;
  }

  public String getOlderAdultId() {
    return olderAdultId;
  }

  public String getMedicationName() {
    return medicationName;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public OmissionCaseStatus getStatus() {
    return status;
  }

  public GracePeriod getGracePeriod() {
    return gracePeriod;
  }

  public Instant getReinforcedReminderSentAt() {
    return reinforcedReminderSentAt;
  }

  public Instant getOmittedAt() {
    return omittedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public List<CareAlert> getAlerts() {
    return Collections.unmodifiableList(alerts);
  }

  public List<EscalationRecord> getEscalations() {
    return Collections.unmodifiableList(escalations);
  }

  /** Restores persisted state without replaying business actions. */
  public static OmissionCase rehydrate(Long id, String intakeId, String olderAdultId, String medicationName, Instant scheduledAt, OmissionCaseStatus status, GracePeriod gracePeriod, Instant reinforcedReminderSentAt, Instant omittedAt, Instant closedAt, List<CareAlert> alerts, List<EscalationRecord> escalations, Instant createdAt, Instant updatedAt) {
    var restored = new OmissionCase();
    restored.id = id;
    restored.intakeId = intakeId;
    restored.olderAdultId = olderAdultId;
    restored.medicationName = medicationName;
    restored.scheduledAt = scheduledAt;
    restored.status = status;
    restored.gracePeriod = gracePeriod;
    restored.reinforcedReminderSentAt = reinforcedReminderSentAt;
    restored.omittedAt = omittedAt;
    restored.closedAt = closedAt;
    restored.alerts = new ArrayList<>(alerts);
    restored.escalations = new ArrayList<>(escalations);
    restored.createdAt = createdAt;
    restored.updatedAt = updatedAt;
    return restored;
  }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
