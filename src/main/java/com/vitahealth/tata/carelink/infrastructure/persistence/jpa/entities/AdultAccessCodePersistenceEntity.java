package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stored adult sign-in code; the value is unique across every issued code. */
@Entity
@Table(
    name = "care_link_adult_access_codes",
    indexes = @Index(name = "idx_adult_access_code_link", columnList = "care_link_id"))
public class AdultAccessCodePersistenceEntity {
  @Id private String id;

  @Column(name = "care_link_id", nullable = false, length = 36)
  private String careLinkId;

  @Column(name = "older_adult_id", nullable = false, length = 36)
  private String olderAdultId;

  @Column(name = "issued_by", nullable = false, length = 36)
  private String issuedBy;

  @Column(name = "code", nullable = false, unique = true, length = 32)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** ADULT_ACCESS or CAREGIVER_INVITATION; null in rows that predate the kind. */
  @Column(name = "kind", length = 30)
  private String kind;

  /** Required by JPA. */
  protected AdultAccessCodePersistenceEntity() {}

  /** Copies complete code evidence. */
  public AdultAccessCodePersistenceEntity(
      String id,
      String careLinkId,
      String olderAdultId,
      String issuedBy,
      String code,
      Instant expiresAt,
      Instant usedAt,
      Instant createdAt,
      String kind) {
    this.kind = kind;
    this.id = id;
    this.careLinkId = careLinkId;
    this.olderAdultId = olderAdultId;
    this.issuedBy = issuedBy;
    this.code = code;
    this.expiresAt = expiresAt;
    this.usedAt = usedAt;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getCareLinkId() {
    return careLinkId;
  }

  public String getOlderAdultId() {
    return olderAdultId;
  }

  public String getIssuedBy() {
    return issuedBy;
  }

  public String getCode() {
    return code;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public String getKind() {
    return kind;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
