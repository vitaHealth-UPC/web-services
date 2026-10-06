package com.tata.omissionescalation.domain.model.aggregates;

import com.tata.omissionescalation.domain.model.entities.CareAlert;
import com.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Life cycle of an unconfirmed intake: grace period, omission, caregiver alert and escalation.
 * The intake and the older adult are referenced by logical id only.
 */
@Entity
public class OmissionCase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long intakeId;

  @Column(nullable = false)
  private Long olderAdultId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OmissionCaseStatus status;

  @Embedded
  private GracePeriod gracePeriod;

  private Instant reinforcedReminderSentAt;

  private Instant omittedAt;

  private Instant closedAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<CareAlert> alerts = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<EscalationRecord> escalations = new ArrayList<>();

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  protected OmissionCase() {
  }

  public OmissionCase(
      Long intakeId,
      Long olderAdultId,
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

  public Long getIntakeId() {
    return intakeId;
  }

  public Long getOlderAdultId() {
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
}
