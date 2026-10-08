package com.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
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
    @Column(name = "subscription_plan_code", length = 40) private String subscriptionPlanCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", length = 40)
    private SubscriptionStatus subscriptionStatus;
    @Column(name = "subscription_renews_at") private Instant subscriptionRenewsAt;

    protected AccountPersistenceEntity() {}

    public AccountPersistenceEntity(
            String id,
            String name,
            String email,
            String passwordHash,
            AccountStatus status,
            String verificationCodeHash,
            Instant verificationExpiresAt,
            String subscriptionPlanCode,
            SubscriptionStatus subscriptionStatus,
            Instant subscriptionRenewsAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.verificationCodeHash = verificationCodeHash;
        this.verificationExpiresAt = verificationExpiresAt;
        this.subscriptionPlanCode = subscriptionPlanCode;
        this.subscriptionStatus = subscriptionStatus;
        this.subscriptionRenewsAt = subscriptionRenewsAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public AccountStatus getStatus() { return status; }
    public String getVerificationCodeHash() { return verificationCodeHash; }
    public Instant getVerificationExpiresAt() { return verificationExpiresAt; }
    public String getSubscriptionPlanCode() { return subscriptionPlanCode; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public Instant getSubscriptionRenewsAt() { return subscriptionRenewsAt; }
}
