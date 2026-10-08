package com.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_account_sessions")
public class AccountSessionPersistenceEntity {
    @Id private String id;
    @Column(name = "account_id", nullable = false, length = 36) private String accountId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    @Column(name = "session_role", length = 24) private String role;
    @Column(name = "care_link_id", length = 36) private String careLinkId;
    public String getRole() { return role; }
    public String getCareLinkId() { return careLinkId; }
    protected AccountSessionPersistenceEntity() {}

    public AccountSessionPersistenceEntity(String accountId, String tokenHash, Instant expiresAt) {
        this(accountId, tokenHash, expiresAt, "CAREGIVER", null);
    }
    public AccountSessionPersistenceEntity(String accountId, String tokenHash, Instant expiresAt, String role, String careLinkId) {
        this.role = role; this.careLinkId = careLinkId;
        this.id = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
}
