package com.tata.omissionescalation.domain.model.entities;

import com.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** Alert generated for the caregiver from an omission, with the result of its delivery. */
@Entity
public class CareAlert {

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

  protected CareAlert() {
  }

  public CareAlert(Instant generatedAt) {
    this.status = AlertStatus.GENERATED;
    this.generatedAt = generatedAt;
  }

  public void markSent(Instant sentAt) {
    this.status = AlertStatus.SENT;
    this.sentAt = sentAt;
    this.failureReason = null;
  }

  public void markFailed(String failureReason) {
    this.status = AlertStatus.FAILED;
    this.failureReason = failureReason;
  }

  public Long getId() {
    return id;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getFailureReason() {
    return failureReason;
  }
}
