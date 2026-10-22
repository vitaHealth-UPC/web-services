package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.GracePeriodPersistenceEmbeddable;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Stores omission lifecycle evidence and owned delivery and escalation history. */
@Entity
@Table(name = "omission_cases")
public class OmissionCasePersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 36)
  private String intakeId;

  @Column(nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OmissionCaseStatus status;

  @Embedded private GracePeriodPersistenceEmbeddable gracePeriod;

  private Instant reinforcedReminderSentAt;

  private Instant omittedAt;

  private Instant closedAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<CareAlertPersistenceEntity> alerts = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<EscalationRecordPersistenceEntity> escalations = new ArrayList<>();

  @Column(updatable = false)
  private Instant createdAt;

  private Instant updatedAt;

  /** Supports JPA hydration and the context persistence assembler. */
  public OmissionCasePersistenceEntity() {}

  /**
   * Applies mutable decisions to the managed row without replacing identity, creation, or history.
   */
  public void applyMutableState(OmissionCasePersistenceEntity source) {
    if (!Objects.equals(id, source.id)
        || !Objects.equals(intakeId, source.intakeId)
        || !Objects.equals(olderAdultId, source.olderAdultId)
        || !Objects.equals(medicationName, source.medicationName)
        || !Objects.equals(scheduledAt, source.scheduledAt)
        || !Objects.equals(gracePeriod, source.gracePeriod))
      throw new IllegalStateException("stored omission identity and source evidence cannot change");
    if (reinforcedReminderSentAt != null
            && !Objects.equals(reinforcedReminderSentAt, source.reinforcedReminderSentAt)
        || omittedAt != null && !Objects.equals(omittedAt, source.omittedAt)
        || closedAt != null && !Objects.equals(closedAt, source.closedAt))
      throw new IllegalStateException("recorded omission timestamps cannot be replaced");
    if ((status == OmissionCaseStatus.CLOSED || status == OmissionCaseStatus.RESOLVED)
            && source.status != status
        || status == OmissionCaseStatus.OMITTED
            && (source.status == OmissionCaseStatus.PENDING
                || source.status == OmissionCaseStatus.RESOLVED)
        || status == OmissionCaseStatus.ESCALATED
            && source.status != OmissionCaseStatus.ESCALATED
            && source.status != OmissionCaseStatus.CLOSED)
      throw new IllegalStateException("stored omission lifecycle cannot move backward");
    validateAlertHistory(source);
    validateEscalationHistory(source);
    status = source.status;
    reinforcedReminderSentAt = source.reinforcedReminderSentAt;
    omittedAt = source.omittedAt;
    closedAt = source.closedAt;
    for (var incoming : source.alerts) {
      if (incoming.getId() == null) alerts.add(incoming);
      else {
        var managed =
            alerts.stream()
                .filter(row -> Objects.equals(row.getId(), incoming.getId()))
                .findFirst()
                .orElseThrow();
        managed.setStatus(incoming.getStatus());
        managed.setSentAt(incoming.getSentAt());
        managed.setFailureReason(incoming.getFailureReason());
      }
    }
    for (var incoming : source.escalations) if (incoming.getId() == null) escalations.add(incoming);
  }

  private void validateAlertHistory(OmissionCasePersistenceEntity source) {
    var seen = new HashSet<Long>();
    for (var incoming : source.alerts) {
      if (incoming.getId() == null) continue;
      var current =
          alerts.stream()
              .filter(row -> Objects.equals(row.getId(), incoming.getId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("alert does not belong to stored omission"));
      if (!seen.add(incoming.getId())
          || !Objects.equals(current.getGeneratedAt(), incoming.getGeneratedAt())
          || current.getStatus() == AlertStatus.SENT
              && (incoming.getStatus() != AlertStatus.SENT
                  || !Objects.equals(current.getSentAt(), incoming.getSentAt())
                  || !Objects.equals(current.getFailureReason(), incoming.getFailureReason())))
        throw new IllegalStateException("stored alert evidence cannot be replaced");
    }
    if (seen.size() != alerts.size())
      throw new IllegalStateException("stored alerts cannot be removed");
  }

  private void validateEscalationHistory(OmissionCasePersistenceEntity source) {
    var seen = new HashSet<Long>();
    for (var incoming : source.escalations) {
      if (incoming.getId() == null) continue;
      var current =
          escalations.stream()
              .filter(row -> Objects.equals(row.getId(), incoming.getId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("escalation does not belong to stored omission"));
      if (!seen.add(incoming.getId())
          || !Objects.equals(current.getLevel(), incoming.getLevel())
          || !Objects.equals(current.getReason(), incoming.getReason())
          || !Objects.equals(current.getTriggeredAt(), incoming.getTriggeredAt()))
        throw new IllegalStateException("stored escalation evidence cannot be replaced");
    }
    if (seen.size() != escalations.size())
      throw new IllegalStateException("stored escalations cannot be removed");
  }

  /** Returns the stored generated identity. */
  public Long getId() {
    return id;
  }

  /**
   * Assigns the stored generated identity during snapshot assembly; managed updates preserve
   * existing evidence.
   */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns the immutable source intake reference. */
  public String getIntakeId() {
    return intakeId;
  }

  /**
   * Assigns the immutable source intake reference during snapshot assembly; managed updates
   * preserve existing evidence.
   */
  public void setIntakeId(String value) {
    this.intakeId = value;
  }

  /** Returns the immutable logical adult reference. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /**
   * Assigns the immutable logical adult reference during snapshot assembly; managed updates
   * preserve existing evidence.
   */
  public void setOlderAdultId(String value) {
    this.olderAdultId = value;
  }

  /** Returns the medication snapshot captured when the case opens. */
  public String getMedicationName() {
    return medicationName;
  }

  /**
   * Assigns the medication snapshot captured when the case opens during snapshot assembly; managed
   * updates preserve existing evidence.
   */
  public void setMedicationName(String value) {
    this.medicationName = value;
  }

  /** Returns the original scheduled intake instant. */
  public Instant getScheduledAt() {
    return scheduledAt;
  }

  /**
   * Assigns the original scheduled intake instant during snapshot assembly; managed updates
   * preserve existing evidence.
   */
  public void setScheduledAt(Instant value) {
    this.scheduledAt = value;
  }

  /** Returns the stored lifecycle state. */
  public OmissionCaseStatus getStatus() {
    return status;
  }

  /** Assigns the stored lifecycle state. */
  public void setStatus(OmissionCaseStatus value) {
    this.status = value;
  }

  /** Returns the stored grace boundaries. */
  public GracePeriodPersistenceEmbeddable getGracePeriod() {
    return gracePeriod;
  }

  /**
   * Assigns the stored grace boundaries during snapshot assembly; managed updates preserve existing
   * evidence.
   */
  public void setGracePeriod(GracePeriodPersistenceEmbeddable value) {
    this.gracePeriod = value;
  }

  /** Returns the nullable first successful reminder instant. */
  public Instant getReinforcedReminderSentAt() {
    return reinforcedReminderSentAt;
  }

  /** Assigns the nullable first successful reminder instant. */
  public void setReinforcedReminderSentAt(Instant value) {
    this.reinforcedReminderSentAt = value;
  }

  /** Returns the nullable definitive omission instant. */
  public Instant getOmittedAt() {
    return omittedAt;
  }

  /** Assigns the nullable definitive omission instant. */
  public void setOmittedAt(Instant value) {
    this.omittedAt = value;
  }

  /** Returns the nullable final closure instant. */
  public Instant getClosedAt() {
    return closedAt;
  }

  /** Assigns the nullable final closure instant. */
  public void setClosedAt(Instant value) {
    this.closedAt = value;
  }

  /** Returns the managed owned alert collection. */
  public List<CareAlertPersistenceEntity> getAlerts() {
    return alerts;
  }

  /** Assigns the managed owned alert collection. */
  public void setAlerts(List<CareAlertPersistenceEntity> value) {
    this.alerts = value;
  }

  /** Returns the managed owned escalation history. */
  public List<EscalationRecordPersistenceEntity> getEscalations() {
    return escalations;
  }

  /** Assigns the managed owned escalation history. */
  public void setEscalations(List<EscalationRecordPersistenceEntity> value) {
    this.escalations = value;
  }

  /** Returns the original technical creation instant. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Assigns the original technical creation instant during snapshot assembly; managed updates
   * preserve existing evidence.
   */
  public void setCreatedAt(Instant value) {
    this.createdAt = value;
  }

  /** Returns the technical instant of the latest deliberate repository write. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Assigns the technical instant of the latest deliberate repository write. */
  public void setUpdatedAt(Instant value) {
    this.updatedAt = value;
  }
}
