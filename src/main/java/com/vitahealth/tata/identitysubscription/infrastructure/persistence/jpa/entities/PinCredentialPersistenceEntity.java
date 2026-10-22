package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

/** Stores one encoded PIN per adult with its persisted authentication failure history. */
@Entity
@Table(
    name = "identity_pin_credentials",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_identity_pin_adult", columnNames = "older_adult_id"))
public class PinCredentialPersistenceEntity {
  @Id private String id;

  @Column(name = "older_adult_id", nullable = false, length = 36)
  private String olderAdultId;

  @Column(name = "pin_hash", nullable = false, length = 100)
  private String pinHash;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  /** Constructor required by JPA hydration. */
  protected PinCredentialPersistenceEntity() {}

  /** Creates the persistence representation without receiving or retaining a plaintext PIN. */
  public PinCredentialPersistenceEntity(
      String id, String olderAdultId, String pinHash, int failedAttempts, Instant lockedUntil) {
    this.id = id;
    this.olderAdultId = olderAdultId;
    this.pinHash = pinHash;
    this.failedAttempts = failedAttempts;
    this.lockedUntil = lockedUntil;
  }

  /**
   * Applies authentication state to the managed credential without changing its owner or identity.
   *
   * @param state replacement snapshot of the same credential
   * @throws IllegalArgumentException when the snapshot belongs to another credential or owner
   */
  public void updateFrom(PinCredentialPersistenceEntity state) {
    Objects.requireNonNull(state, "state");
    if (!Objects.equals(id, state.id) || !Objects.equals(olderAdultId, state.olderAdultId)) {
      throw new IllegalArgumentException("credential identity and owner cannot change");
    }
    pinHash = state.pinHash;
    failedAttempts = state.failedAttempts;
    lockedUntil = state.lockedUntil;
  }

  /** Returns the credential identity. */
  public String getId() {
    return id;
  }

  /** Returns the adult owning this credential. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the encoded PIN for domain reconstruction only. */
  public String getPinHash() {
    return pinHash;
  }

  /** Returns stored failed-attempt history. */
  public int getFailedAttempts() {
    return failedAttempts;
  }

  /** Returns the temporary authentication lock expiration. */
  public Instant getLockedUntil() {
    return lockedUntil;
  }
}
