package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Stores treatment evidence; business lifecycle rules remain in the domain aggregate. */
@Entity
@Table(name = "treatment_treatments")
public class TreatmentPersistenceEntity {
  @Id private String id;

  @Column(name = "older_adult_id", nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false, length = 160)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TreatmentStatus status;

  @Column(name = "medication_id", length = 36)
  private String medicationId;

  @Column(length = 100)
  private String dose;

  @Column(length = 100)
  private String frequency;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "treatment_schedule_times",
      joinColumns = @JoinColumn(name = "treatment_id"))
  @Column(name = "scheduled_time", nullable = false)
  @OrderColumn(name = "schedule_order")
  private List<LocalTime> scheduledTimes = new ArrayList<>();

  @Column(length = 500)
  private String instructions;

  @Column(name = "reminder_lead_minutes")
  private Integer reminderLeadMinutes;

  @Column(name = "follow_up_delay_minutes")
  private Integer followUpDelayMinutes;

  public Integer getFollowUpDelayMinutes() {
    return followUpDelayMinutes;
  }

  public void setFollowUpDelayMinutes(Integer minutes) {
    followUpDelayMinutes = minutes;
  }

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "recurrence_mode", length = 30)
  private String recurrenceMode;

  @Column(name = "recurrence_zone", length = 100)
  private String recurrenceZone;

  @Column(name = "recurrence_anchor")
  private Instant recurrenceAnchor;

  /** Required by JPA when restoring stored fields. */
  protected TreatmentPersistenceEntity() {}

  /** Captures aggregate state without generating identity or registration timestamps. */
  public TreatmentPersistenceEntity(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      Integer reminderLeadMinutes,
      Instant createdAt) {
    this.id = id;
    this.olderAdultId = olderAdultId;
    this.name = name;
    this.status = status;
    this.medicationId = medicationId;
    this.dose = dose;
    this.frequency = frequency;
    this.scheduledTimes =
        scheduledTimes == null ? new ArrayList<>() : new ArrayList<>(scheduledTimes);
    this.instructions = instructions;
    this.reminderLeadMinutes = reminderLeadMinutes;
    this.createdAt = createdAt;
  }

  /** Captures explicit recurrence metadata together with the original aggregate fields. */
  public TreatmentPersistenceEntity(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      Integer reminderLeadMinutes,
      Instant createdAt,
      String recurrenceMode,
      String recurrenceZone,
      Instant recurrenceAnchor) {
    this(
        id,
        olderAdultId,
        name,
        status,
        medicationId,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        createdAt);
    this.recurrenceMode = recurrenceMode;
    this.recurrenceZone = recurrenceZone;
    this.recurrenceAnchor = recurrenceAnchor;
  }

  /** Returns the stored recurrence mode, or null for a pre-migration schedule. */
  public String getRecurrenceMode() {
    return recurrenceMode;
  }

  /** Returns the explicit named calendar zone, or null with legacy metadata. */
  public String getRecurrenceZone() {
    return recurrenceZone;
  }

  /** Returns the retained first occurrence of an elapsed interval. */
  public Instant getRecurrenceAnchor() {
    return recurrenceAnchor;
  }

  /** Returns the original aggregate identity. */
  public String getId() {
    return id;
  }

  /** Returns the adult owning the stored aggregate. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the stored display name. */
  public String getName() {
    return name;
  }

  /** Returns the persisted lifecycle state. */
  public TreatmentStatus getStatus() {
    return status;
  }

  /** Returns the configured medication identity, or null for an empty draft. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Returns the configured dose text, or null for an empty draft. */
  public String getDose() {
    return dose;
  }

  /** Returns the configured frequency text, or null for an empty draft. */
  public String getFrequency() {
    return frequency;
  }

  /** Returns an immutable snapshot of the stored local schedule. */
  public List<LocalTime> getScheduledTimes() {
    return List.copyOf(scheduledTimes);
  }

  /** Returns stored optional regimen instructions. */
  public String getInstructions() {
    return instructions;
  }

  /** Returns explicit reminder evidence, or null for an empty draft. */
  public Integer getReminderLeadMinutes() {
    return reminderLeadMinutes;
  }

  /** Returns the original registration instant. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
