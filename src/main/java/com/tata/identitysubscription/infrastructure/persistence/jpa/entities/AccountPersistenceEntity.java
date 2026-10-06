package com.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "identity_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class AccountPersistenceEntity {
    @Id private String id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 200) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AccountStatus status;
    @Column(name = "verification_code_hash", length = 100) private String verificationCodeHash;
    @Column(name = "verification_expires_at") private Instant verificationExpiresAt;

    protected AccountPersistenceEntity() {}

    public AccountPersistenceEntity(
            String id, String name, String email, String passwordHash, AccountStatus status,
            String verificationCodeHash, Instant verificationExpiresAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.verificationCodeHash = verificationCodeHash;
        this.verificationExpiresAt = verificationExpiresAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public AccountStatus getStatus() { return status; }
    public String getVerificationCodeHash() { return verificationCodeHash; }
    public Instant getVerificationExpiresAt() { return verificationExpiresAt; }
}
