package com.tata.identitysubscription.domain.model.aggregates;

import static com.tata.shared.domain.validation.DomainText.requireText;

import com.tata.identitysubscription.domain.model.entities.Plan;
import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import com.tata.identitysubscription.domain.services.PlanCatalog;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Account {
    private static final Duration SUBSCRIPTION_CYCLE = Duration.ofDays(30);

    private final String id;
    private final String name;
    private final EmailAddress email;
    private final String passwordHash;
    private AccountStatus status;
    private String verificationCodeHash;
    private Instant verificationExpiresAt;
    private String currentPlanCode;
    private SubscriptionStatus subscriptionStatus;
    private Instant subscriptionRenewsAt;

    private Account(
            String id,
            String name,
            EmailAddress email,
            String passwordHash,
            AccountStatus status,
            String verificationCodeHash,
            Instant verificationExpiresAt,
            String currentPlanCode,
            SubscriptionStatus subscriptionStatus,
            Instant subscriptionRenewsAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "name");
        this.email = Objects.requireNonNull(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status);
        this.verificationCodeHash = verificationCodeHash;
        this.verificationExpiresAt = verificationExpiresAt;
        this.currentPlanCode = requireText(currentPlanCode, "currentPlanCode").toUpperCase(java.util.Locale.ROOT);
        this.subscriptionStatus = Objects.requireNonNull(subscriptionStatus);
        this.subscriptionRenewsAt = subscriptionRenewsAt;
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
                now.plus(verificationTtl),
                PlanCatalog.ESSENTIAL,
                SubscriptionStatus.ACTIVE,
                now.plus(SUBSCRIPTION_CYCLE)
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
        return rehydrate(
                id,
                name,
                email,
                passwordHash,
                status,
                verificationCodeHash,
                verificationExpiresAt,
                PlanCatalog.ESSENTIAL,
                SubscriptionStatus.ACTIVE,
                null
        );
    }

    public static Account rehydrate(
            String id,
            String name,
            EmailAddress email,
            String passwordHash,
            AccountStatus status,
            String verificationCodeHash,
            Instant verificationExpiresAt,
            String currentPlanCode,
            SubscriptionStatus subscriptionStatus,
            Instant subscriptionRenewsAt
    ) {
        String safePlan = currentPlanCode == null || currentPlanCode.isBlank()
                ? PlanCatalog.ESSENTIAL
                : currentPlanCode;
        SubscriptionStatus safeSubscriptionStatus = subscriptionStatus == null
                ? SubscriptionStatus.ACTIVE
                : subscriptionStatus;
        return new Account(
                id,
                name,
                email,
                passwordHash,
                status,
                verificationCodeHash,
                verificationExpiresAt,
                safePlan,
                safeSubscriptionStatus,
                subscriptionRenewsAt
        );
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

    /**
     * Changes the active plan without extending the renewal date on an idempotent retry.
     */
    public boolean changeSubscription(Plan plan, Instant now) {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(now);
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("account is not active");
        }
        if (subscriptionStatus == SubscriptionStatus.ACTIVE
                && currentPlanCode.equals(plan.code())) {
            return false;
        }
        currentPlanCode = plan.code();
        subscriptionStatus = SubscriptionStatus.ACTIVE;
        subscriptionRenewsAt = now.plus(SUBSCRIPTION_CYCLE);
        return true;
    }


    public String id() { return id; }
    public String name() { return name; }
    public EmailAddress email() { return email; }
    public String passwordHash() { return passwordHash; }
    public AccountStatus status() { return status; }
    public String verificationCodeHash() { return verificationCodeHash; }
    public Instant verificationExpiresAt() { return verificationExpiresAt; }
    public String currentPlanCode() { return currentPlanCode; }
    public SubscriptionStatus subscriptionStatus() { return subscriptionStatus; }
    public Instant subscriptionRenewsAt() { return subscriptionRenewsAt; }
}
