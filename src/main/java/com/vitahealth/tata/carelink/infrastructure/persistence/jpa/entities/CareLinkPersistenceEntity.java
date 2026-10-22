package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

/**
 * Stores relationship lifecycle evidence separately from Identity account and profile persistence.
 */
@Entity
@Table(
    name = "care_links",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_care_link_reserved_code", columnNames = "reserved_linking_code"),
      @UniqueConstraint(
          name = "uk_care_link_reserved_members",
          columnNames = {"reserved_caregiver_id", "reserved_older_adult_id"})
    },
    indexes = {
      @Index(name = "idx_care_links_code", columnList = "linking_code"),
      @Index(
          name = "idx_care_links_adult_recipients",
          columnList = "older_adult_id,status,caregiver_id"),
      @Index(name = "idx_care_links_members", columnList = "caregiver_id,older_adult_id")
    })
public class CareLinkPersistenceEntity {
  @Id private String id;

  @Column(name = "caregiver_id", nullable = false)
  private String caregiverId;

  @Column(name = "older_adult_id", nullable = false)
  private String olderAdultId;

  @Column(name = "reserved_caregiver_id", length = 36)
  private String reservedCaregiverId;

  @Column(name = "reserved_older_adult_id", length = 36)
  private String reservedOlderAdultId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private CareLinkStatus status;

  @Column(name = "linking_code", nullable = false, length = 32)
  private String linkingCode;

  @Column(name = "reserved_linking_code", length = 32)
  private String reservedLinkingCode;

  @Column(name = "code_expires_at", nullable = false)
  private Instant codeExpiresAt;

  @Column(name = "code_used_at")
  private Instant codeUsedAt;

  @Column(name = "consent_granted", nullable = false)
  private boolean consentGranted;

  @Column(name = "consent_recorded_at")
  private Instant consentRecordedAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Constructor required for JPA reconstruction. */
  protected CareLinkPersistenceEntity() {}

  /** Creates the context persistence representation with explicit domain occurrence timestamps. */
  public CareLinkPersistenceEntity(
      String id,
      String caregiverId,
      String olderAdultId,
      CareLinkStatus status,
      String linkingCode,
      Instant codeExpiresAt,
      Instant codeUsedAt,
      boolean consentGranted,
      Instant consentRecordedAt,
      Instant confirmedAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.caregiverId = caregiverId;
    this.olderAdultId = olderAdultId;
    this.status = status;
    this.reservedCaregiverId = status == CareLinkStatus.REVOKED ? null : caregiverId;
    this.reservedOlderAdultId = status == CareLinkStatus.REVOKED ? null : olderAdultId;
    this.linkingCode = linkingCode;
    this.reservedLinkingCode =
        status == CareLinkStatus.PENDING && codeUsedAt == null ? linkingCode : null;
    this.codeExpiresAt = codeExpiresAt;
    this.codeUsedAt = codeUsedAt;
    this.consentGranted = consentGranted;
    this.consentRecordedAt = consentRecordedAt;
    this.confirmedAt = confirmedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Applies lifecycle state without replacing identity, members, code or creation evidence. */
  public void updateFrom(CareLinkPersistenceEntity state) {
    Objects.requireNonNull(state, "state");
    if (!Objects.equals(id, state.id)
        || !Objects.equals(caregiverId, state.caregiverId)
        || !Objects.equals(olderAdultId, state.olderAdultId)
        || !Objects.equals(linkingCode, state.linkingCode)
        || !Objects.equals(codeExpiresAt, state.codeExpiresAt)
        || !Objects.equals(createdAt, state.createdAt)) {
      throw new IllegalArgumentException(
          "relationship identity, members and issuance evidence cannot change");
    }
    if (Objects.requireNonNull(state.updatedAt, "updatedAt").isBefore(updatedAt)
        || codeUsedAt != null && !Objects.equals(codeUsedAt, state.codeUsedAt)
        || confirmedAt != null && !Objects.equals(confirmedAt, state.confirmedAt)
        || status == CareLinkStatus.REVOKED && state.status != CareLinkStatus.REVOKED
        || status == CareLinkStatus.CONFIRMED
            && state.status != CareLinkStatus.CONFIRMED
            && state.status != CareLinkStatus.REVOKED
        || status == CareLinkStatus.AWAITING_CONSENT && state.status == CareLinkStatus.PENDING) {
      throw new IllegalArgumentException(
          "relationship history cannot be replaced by an older lifecycle snapshot");
    }
    status = state.status;
    codeUsedAt = state.codeUsedAt;
    consentGranted = state.consentGranted;
    consentRecordedAt = state.consentRecordedAt;
    confirmedAt = state.confirmedAt;
    updatedAt = state.updatedAt;
    reservedLinkingCode = state.reservedLinkingCode;
    reservedCaregiverId = state.reservedCaregiverId;
    reservedOlderAdultId = state.reservedOlderAdultId;
  }

  /** Releases expired pending code and pair reservations while keeping accepted pairs reserved. */
  public void releaseReservationIfExpired(Instant now) {
    if (status == CareLinkStatus.PENDING
        && !codeExpiresAt.isAfter(Objects.requireNonNull(now, "now"))) {
      reservedLinkingCode = null;
      reservedCaregiverId = null;
      reservedOlderAdultId = null;
    }
  }

  /** Returns relationship identity. */
  public String getId() {
    return id;
  }

  /** Returns caregiver logical identity. */
  public String getCaregiverId() {
    return caregiverId;
  }

  /** Returns adult profile logical identity. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns stored lifecycle status. */
  public CareLinkStatus getStatus() {
    return status;
  }

  /** Returns linking-code evidence. */
  public String getLinkingCode() {
    return linkingCode;
  }

  /** Returns the immutable code expiration evidence. */
  public Instant getCodeExpiresAt() {
    return codeExpiresAt;
  }

  /** Returns code consumption time, or null before acceptance. */
  public Instant getCodeUsedAt() {
    return codeUsedAt;
  }

  /** Returns the explicit consent decision. */
  public boolean isConsentGranted() {
    return consentGranted;
  }

  /** Returns adult decision time, or null while undecided. */
  public Instant getConsentRecordedAt() {
    return consentRecordedAt;
  }

  /** Returns confirmation time, or null before confirmation. */
  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  /** Returns the relationship's domain creation time. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns its last domain transition time. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
