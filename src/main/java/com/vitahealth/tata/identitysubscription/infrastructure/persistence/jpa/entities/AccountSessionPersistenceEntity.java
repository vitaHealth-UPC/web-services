package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Revocable credential evidence with a stable session ID and a digest instead of the raw token. */
@Entity
@Table(
    name = "identity_account_sessions",
    indexes =
        @Index(
            name = "idx_identity_session_subject_role",
            columnList = "account_id,session_role,expires_at"))
public class AccountSessionPersistenceEntity {
  @Id private String id;

  @Column(name = "account_id", nullable = false, length = 36)
  private String accountId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "session_role", length = 24)
  private String role;

  @Column(name = "care_link_id", length = 36)
  private String careLinkId;

  /** Returns the persisted role evidence checked against signed session claims. */
  public String getRole() {
    return role;
  }

  /** Returns the relationship scope of adult or setup credentials. */
  public String getCareLinkId() {
    return careLinkId;
  }

  /** Constructor required by JPA hydration. */
  protected AccountSessionPersistenceEntity() {}

  /** Creates legacy caregiver credential evidence without a relationship scope. */
  public AccountSessionPersistenceEntity(String accountId, String tokenHash, Instant expiresAt) {
    this(accountId, tokenHash, expiresAt, "CAREGIVER", null);
  }

  /** Creates revocable evidence with a generated session identity for opaque credentials. */
  public AccountSessionPersistenceEntity(
      String accountId, String tokenHash, Instant expiresAt, String role, String careLinkId) {
    this(UUID.randomUUID().toString(), accountId, tokenHash, expiresAt, role, careLinkId);
  }

  /** Preserves the session identity signed into the JWT while storing only its credential hash. */
  public AccountSessionPersistenceEntity(
      String id,
      String accountId,
      String tokenHash,
      Instant expiresAt,
      String role,
      String careLinkId) {
    this.role = role;
    this.careLinkId = careLinkId;
    this.id = id;
    this.accountId = accountId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  /** Returns the stable session identity. */
  public String getId() {
    return id;
  }

  /** Returns the caregiver or adult subject owning the credential. */
  public String getAccountId() {
    return accountId;
  }

  /** Returns the credential digest used for lookup; no raw bearer token is stored. */
  public String getTokenHash() {
    return tokenHash;
  }

  /** Returns the stored credential expiration. */
  public Instant getExpiresAt() {
    return expiresAt;
  }
}
