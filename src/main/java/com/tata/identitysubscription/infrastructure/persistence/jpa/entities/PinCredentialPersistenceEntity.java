package com.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "identity_pin_credentials",
        uniqueConstraints = @UniqueConstraint(columnNames = "older_adult_id")
)
public class PinCredentialPersistenceEntity {
    @Id private String id;
    @Column(name = "older_adult_id", nullable = false, length = 36) private String olderAdultId;
    @Column(name = "pin_hash", nullable = false, length = 100) private String pinHash;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;

    protected PinCredentialPersistenceEntity() {}

    public PinCredentialPersistenceEntity(
            String id, String olderAdultId, String pinHash, int failedAttempts, Instant lockedUntil
    ) {
        this.id = id;
        this.olderAdultId = olderAdultId;
        this.pinHash = pinHash;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    public String getId() { return id; }
    public String getOlderAdultId() { return olderAdultId; }
    public String getPinHash() { return pinHash; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
}
