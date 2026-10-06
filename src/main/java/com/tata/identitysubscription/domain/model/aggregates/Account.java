package com.tata.identitysubscription.domain.model.aggregates;

import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Account {
    private final String id;
    private final String name;
    private final EmailAddress email;
    private final String passwordHash;
    private AccountStatus status;
    private String verificationCodeHash;
    private Instant verificationExpiresAt;

    private Account(
            String id,
            String name,
            EmailAddress email,
            String passwordHash,
            AccountStatus status,
            String verificationCodeHash,
            Instant verificationExpiresAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "name");
        this.email = Objects.requireNonNull(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status);
        this.verificationCodeHash = verificationCodeHash;
        this.verificationExpiresAt = verificationExpiresAt;
    }

    public static Account register(
            String name,
            EmailAddress email,
            String passwordHash,
            String verificationCodeHash,
            Instant now,
            Duration verificationTtl
    ) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(verificationTtl);
        return new Account(
                UUID.randomUUID().toString(),
                name,
                email,
                passwordHash,
                AccountStatus.PENDING_VERIFICATION,
                requireText(verificationCodeHash, "verificationCodeHash"),
                now.plus(verificationTtl)
        );
    }

    public static Account rehydrate(
            String id,
            String name,
            EmailAddress email,
            String passwordHash,
            AccountStatus status,
            String verificationCodeHash,
            Instant verificationExpiresAt
    ) {
        return new Account(id, name, email, passwordHash, status, verificationCodeHash, verificationExpiresAt);
    }

    public boolean canCompleteVerification(Instant now) {
        return status == AccountStatus.PENDING_VERIFICATION
                && verificationExpiresAt != null
                && now.isBefore(verificationExpiresAt);
    }

    public void completeVerification() {
        if (status != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("account is not pending verification");
        }
        status = AccountStatus.ACTIVE;
        verificationCodeHash = null;
        verificationExpiresAt = null;
    }

    public void renewVerificationCode(
            String newVerificationCodeHash,
            Instant now,
            Duration verificationTtl
    ) {
        if (status != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("account is not pending verification");
        }
        Objects.requireNonNull(now);
        Objects.requireNonNull(verificationTtl);
        verificationCodeHash = requireText(newVerificationCodeHash, "verificationCodeHash");
        verificationExpiresAt = now.plus(verificationTtl);
    }

    public boolean canAuthenticate() {
        return status == AccountStatus.ACTIVE;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public String id() { return id; }
    public String name() { return name; }
    public EmailAddress email() { return email; }
    public String passwordHash() { return passwordHash; }
    public AccountStatus status() { return status; }
    public String verificationCodeHash() { return verificationCodeHash; }
    public Instant verificationExpiresAt() { return verificationExpiresAt; }
}
