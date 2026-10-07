package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "alert_summaries")
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


  public AlertSummaryPersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getIntakeId() { return intakeId; }
  public void setIntakeId(String value) { this.intakeId = value; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String value) { this.medicationName = value; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant value) { this.scheduledAt = value; }
  public String getReason() { return reason; }
  public void setReason(String value) { this.reason = value; }
  public AlertStatus getStatus() { return status; }
  public void setStatus(AlertStatus value) { this.status = value; }
  public Instant getOpenedAt() { return openedAt; }
  public void setOpenedAt(Instant value) { this.openedAt = value; }
  public Instant getClosedAt() { return closedAt; }
  public void setClosedAt(Instant value) { this.closedAt = value; }
}
