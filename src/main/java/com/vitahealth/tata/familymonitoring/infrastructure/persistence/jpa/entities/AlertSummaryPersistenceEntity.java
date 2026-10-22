package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Stores original omission evidence and its caregiver follow-up state as an owned child. */
@Entity
@Table(
    name = "alert_summaries",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_follow_up_omitted_intake",
            columnNames = {"family_monitor_id", "intake_id"}))
public class AlertSummaryPersistenceEntity {

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

  /** Required by the persistence provider. */
  public AlertSummaryPersistenceEntity() {}

  /** Returns stored row identity. */
  public Long getId() {
    return id;
  }

  /** Copies stored row identity during managed persistence assembly or update. */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns original omitted intake reference. */
  public String getIntakeId() {
    return intakeId;
  }

  /** Copies original omitted intake reference during managed persistence assembly or update. */
  public void setIntakeId(String value) {
    this.intakeId = value;
  }

  /** Returns medication name recorded in source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /**
   * Copies medication name recorded in source evidence during managed persistence assembly or
   * update.
   */
  public void setMedicationName(String value) {
    this.medicationName = value;
  }

  /** Returns original intake schedule instant. */
  public Instant getScheduledAt() {
    return scheduledAt;
  }

  /** Copies original intake schedule instant during managed persistence assembly or update. */
  public void setScheduledAt(Instant value) {
    this.scheduledAt = value;
  }

  /** Returns original omission reason. */
  public String getReason() {
    return reason;
  }

  /** Copies original omission reason during managed persistence assembly or update. */
  public void setReason(String value) {
    this.reason = value;
  }

  /** Returns caregiver follow-up state. */
  public AlertStatus getStatus() {
    return status;
  }

  /** Copies caregiver follow-up state during managed persistence assembly or update. */
  public void setStatus(AlertStatus value) {
    this.status = value;
  }

  /** Returns source omission occurrence instant. */
  public Instant getOpenedAt() {
    return openedAt;
  }

  /** Copies source omission occurrence instant during managed persistence assembly or update. */
  public void setOpenedAt(Instant value) {
    this.openedAt = value;
  }

  /** Returns closure instant, absent before explicit closure. */
  public Instant getClosedAt() {
    return closedAt;
  }

  /**
   * Copies closure instant, absent before explicit closure during managed persistence assembly or
   * update.
   */
  public void setClosedAt(Instant value) {
    this.closedAt = value;
  }
}
