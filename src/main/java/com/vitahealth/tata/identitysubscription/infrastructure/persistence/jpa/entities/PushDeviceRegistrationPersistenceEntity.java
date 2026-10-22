package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

/** Versioned device address with unique token fingerprint and immutable session ownership. */
@Entity
@Table(
    name = "identity_push_devices",
    indexes = @Index(name = "idx_identity_push_device_session", columnList = "session_id"),
    uniqueConstraints =
        @UniqueConstraint(name = "uk_identity_push_device_token", columnNames = "token_hash"))
public class PushDeviceRegistrationPersistenceEntity {
  @Id
  @Column(length = 36)
  private String installationId;

  @Column(nullable = false)
  private String sessionId;

  @Column(nullable = false, length = 4096)
  private String token;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor used only by JPA hydration. */
  protected PushDeviceRegistrationPersistenceEntity() {}

  /** Creates stored evidence with an opaque address fingerprint supplied by the assembler. */
  public PushDeviceRegistrationPersistenceEntity(
      String installationId,
      String sessionId,
      String token,
      String tokenHash,
      Instant createdAt,
      Instant updatedAt) {
    this.installationId = installationId;
    this.sessionId = sessionId;
    this.token = token;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Applies a rotation without replacing ownership or creation evidence. */
  public void rotate(String token, String tokenHash, Instant updatedAt) {
    this.token = token;
    this.tokenHash = tokenHash;
    this.updatedAt = updatedAt;
  }

  /** Returns installation identity. */
  public String getInstallationId() {
    return installationId;
  }

  /** Returns the stable revocable session reference. */
  public String getSessionId() {
    return sessionId;
  }

  /** Returns the opaque address only for persistence mapping and authorized dispatch. */
  public String getToken() {
    return token;
  }

  /** Returns original registration time. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the most recent accepted rotation time. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
