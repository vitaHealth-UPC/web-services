package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_password_recoveries")
public class PasswordRecoveryPersistenceEntity {
  @Id
  @Column(name = "account_id")
  private String accountId;

  @Column(name = "code_hash", nullable = false, length = 255)
  private String codeHash;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  protected PasswordRecoveryPersistenceEntity() {}

  public PasswordRecoveryPersistenceEntity(
      String accountId,
      String codeHash,
      Instant issuedAt,
      Instant expiresAt,
      int failedAttempts,
      Instant consumedAt) {
    this.accountId = accountId;
    this.codeHash = codeHash;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.failedAttempts = failedAttempts;
    this.consumedAt = consumedAt;
  }

  public void updateFrom(PasswordRecoveryPersistenceEntity state) {
    if (!Objects.equals(accountId, state.accountId))
      throw new IllegalArgumentException("Cannot replace recovery account identity");
    codeHash = state.codeHash;
    issuedAt = state.issuedAt;
    expiresAt = state.expiresAt;
    failedAttempts = state.failedAttempts;
    consumedAt = state.consumedAt;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }
}
