package com.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.*;
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
@Table(name = "care_alerts")
public class CareAlertPersistenceEntity {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertStatus status;

  @Column(nullable = false)
  private Instant generatedAt;

  private Instant sentAt;

  private String failureReason;


  public CareAlertPersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public AlertStatus getStatus() { return status; }
  public void setStatus(AlertStatus value) { this.status = value; }
  public Instant getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(Instant value) { this.generatedAt = value; }
  public Instant getSentAt() { return sentAt; }
  public void setSentAt(Instant value) { this.sentAt = value; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String value) { this.failureReason = value; }
}
