package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Retains approval and withdrawal evidence after the current consent state becomes revoked. */
@Entity
@Table(name = "care_link_consent_withdrawals")
public class ConsentWithdrawalEvidenceEntity {
  @Id
  @Column(name = "care_link_id", length = 36)
  private String careLinkId;

  @Column(name = "caregiver_id", nullable = false, updatable = false, length = 36)
  private String caregiverId;

  @Column(name = "older_adult_id", nullable = false, updatable = false, length = 36)
  private String olderAdultId;

  @Column(name = "approved_at", nullable = false, updatable = false)
  private Instant approvedAt;

  @Column(name = "withdrawn_at", nullable = false, updatable = false)
  private Instant withdrawnAt;

  /** Constructor required by JPA hydration. */
  protected ConsentWithdrawalEvidenceEntity() {}

  /** Creates immutable evidence for the relationship's single confirmed-consent withdrawal. */
  public ConsentWithdrawalEvidenceEntity(
      String careLinkId,
      String caregiverId,
      String olderAdultId,
      Instant approvedAt,
      Instant withdrawnAt) {
    this.careLinkId = careLinkId;
    this.caregiverId = caregiverId;
    this.olderAdultId = olderAdultId;
    this.approvedAt = approvedAt;
    this.withdrawnAt = withdrawnAt;
  }

  /** Returns the withdrawn relationship identity. */
  public String getCareLinkId() {
    return careLinkId;
  }

  /** Returns the historical caregiver logical identity. */
  public String getCaregiverId() {
    return caregiverId;
  }

  /** Returns the adult whose decision withdrew consent. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the original approval evidence preserved before changing current consent. */
  public Instant getApprovedAt() {
    return approvedAt;
  }

  /** Returns withdrawal decision time. */
  public Instant getWithdrawnAt() {
    return withdrawnAt;
  }
}
