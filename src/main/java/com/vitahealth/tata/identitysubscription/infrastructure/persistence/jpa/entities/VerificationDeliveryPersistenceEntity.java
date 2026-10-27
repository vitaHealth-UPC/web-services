package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.CredentialEmailPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

/** Durable encrypted delivery intention committed atomically with its account challenge. */
@Entity
@Table(name = "identity_verification_deliveries")
public class VerificationDeliveryPersistenceEntity {
  @Id private String id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  @ColumnDefault("'ACCOUNT_VERIFICATION'")
  private CredentialEmailPurpose purpose = CredentialEmailPurpose.ACCOUNT_VERIFICATION;

  public CredentialEmailPurpose purpose() {
    return purpose;
  }

  @Column(nullable = false)
  private String accountId;

  @Column(nullable = false, length = 200)
  private String email;

  @Column(nullable = false, length = 100)
  private String challengeHash;

  @Column(length = 2048)
  private String encryptedCode;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Instant nextAttemptAt;

  @Column(nullable = false)
  private int attempts;

  @Column(length = 200)
  private String providerId;

  @Column(nullable = false)
  private boolean completed;

  private String leaseToken;

  protected VerificationDeliveryPersistenceEntity() {}

  /** Creates an immediately eligible request; the plaintext code is never retained. */
  public VerificationDeliveryPersistenceEntity(
      String id,
      String accountId,
      String email,
      String challengeHash,
      String encryptedCode,
      Instant expiresAt,
      Instant now) {
    this(
        id,
        accountId,
        email,
        challengeHash,
        encryptedCode,
        expiresAt,
        now,
        CredentialEmailPurpose.ACCOUNT_VERIFICATION);
  }

  public VerificationDeliveryPersistenceEntity(
      String id,
      String accountId,
      String email,
      String challengeHash,
      String encryptedCode,
      Instant expiresAt,
      Instant now,
      CredentialEmailPurpose purpose) {
    this.purpose = java.util.Objects.requireNonNull(purpose);
    this.id = id;
    this.accountId = accountId;
    this.email = email;
    this.challengeHash = challengeHash;
    this.encryptedCode = encryptedCode;
    this.expiresAt = expiresAt;
    this.nextAttemptAt = now;
  }

  /** Erases the recoverable secret after acceptance or cancellation. */
  public void complete(String providerId) {
    this.providerId = providerId;
    this.encryptedCode = null;
    this.completed = true;
    this.leaseToken = null;
  }

  /** Schedules another attempt with bounded exponential delay. */
  public void retry(Instant now) {
    attempts++;
    nextAttemptAt = now.plusSeconds(Math.min(300, 5L << Math.min(attempts, 6)));
    leaseToken = null;
  }

  /** Reserves a due intention until the worker finishes or the reservation expires. */
  public boolean claim(String token, Instant now) {
    if (completed || nextAttemptAt.isAfter(now)) {
      return false;
    }
    leaseToken = token;
    nextAttemptAt = now.plusSeconds(60);
    return true;
  }

  /** Prevents a stale worker from overwriting the result of a newer reservation. */
  public boolean isOwnedBy(String token) {
    return !completed && token != null && token.equals(leaseToken);
  }

  /** Returns the stable idempotency identity. */
  public String id() {
    return id;
  }

  /** Returns the owner used to revalidate the challenge. */
  public String accountId() {
    return accountId;
  }

  /** Returns the recipient for this fixed payload. */
  public String email() {
    return email;
  }

  /** Returns the persisted challenge snapshot. */
  public String challengeHash() {
    return challengeHash;
  }

  /** Returns ciphertext for the delivery worker only. */
  public String encryptedCode() {
    return encryptedCode;
  }

  /** Returns the original challenge expiration. */
  public Instant expiresAt() {
    return expiresAt;
  }
}
