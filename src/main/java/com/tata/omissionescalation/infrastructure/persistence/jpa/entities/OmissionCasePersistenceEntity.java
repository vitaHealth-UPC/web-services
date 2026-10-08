package com.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.*;
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
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

  @Embedded
  private GracePeriodPersistenceEmbeddable gracePeriod;

  private Instant reinforcedReminderSentAt;

  private Instant omittedAt;

  private Instant closedAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<CareAlertPersistenceEntity> alerts = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "omission_case_id")
  private List<EscalationRecordPersistenceEntity> escalations = new ArrayList<>();

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;


  public OmissionCasePersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getIntakeId() { return intakeId; }
  public void setIntakeId(String value) { this.intakeId = value; }
  public String getOlderAdultId() { return olderAdultId; }
  public void setOlderAdultId(String value) { this.olderAdultId = value; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String value) { this.medicationName = value; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant value) { this.scheduledAt = value; }
  public OmissionCaseStatus getStatus() { return status; }
  public void setStatus(OmissionCaseStatus value) { this.status = value; }
  public GracePeriodPersistenceEmbeddable getGracePeriod() { return gracePeriod; }
  public void setGracePeriod(GracePeriodPersistenceEmbeddable value) { this.gracePeriod = value; }
  public Instant getReinforcedReminderSentAt() { return reinforcedReminderSentAt; }
  public void setReinforcedReminderSentAt(Instant value) { this.reinforcedReminderSentAt = value; }
  public Instant getOmittedAt() { return omittedAt; }
  public void setOmittedAt(Instant value) { this.omittedAt = value; }
  public Instant getClosedAt() { return closedAt; }
  public void setClosedAt(Instant value) { this.closedAt = value; }
  public List<CareAlertPersistenceEntity> getAlerts() { return alerts; }
  public void setAlerts(List<CareAlertPersistenceEntity> value) { this.alerts = value; }
  public List<EscalationRecordPersistenceEntity> getEscalations() { return escalations; }
  public void setEscalations(List<EscalationRecordPersistenceEntity> value) { this.escalations = value; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant value) { this.createdAt = value; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
