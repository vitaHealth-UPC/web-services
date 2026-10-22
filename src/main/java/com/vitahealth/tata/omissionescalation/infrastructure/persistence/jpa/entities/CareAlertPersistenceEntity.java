package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores owned caregiver-alert delivery evidence independently of domain transitions. */
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

  /** Supports JPA hydration and the context persistence assembler. */
  public CareAlertPersistenceEntity() {}

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

  /** Returns the stored lifecycle state. */
  public AlertStatus getStatus() {
    return status;
  }

  /** Assigns the stored lifecycle state. */
  public void setStatus(AlertStatus value) {
    this.status = value;
  }

  /** Returns the original business alert generation instant. */
  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /** Assigns the original business alert generation instant. */
  public void setGeneratedAt(Instant value) {
    this.generatedAt = value;
  }

  /** Returns the nullable successful send instant. */
  public Instant getSentAt() {
    return sentAt;
  }

  /** Assigns the nullable successful send instant. */
  public void setSentAt(Instant value) {
    this.sentAt = value;
  }

  /** Returns the nullable application-owned delivery failure category. */
  public String getFailureReason() {
    return failureReason;
  }

  /** Assigns the nullable application-owned delivery failure category. */
  public void setFailureReason(String value) {
    this.failureReason = value;
  }
}
