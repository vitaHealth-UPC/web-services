package com.vitahealth.tata.identitysubscription.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.identitysubscription.domain.model.entities.Plan;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.VerificationChallenge;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Owns account activation, verification challenge state and subscription transitions. */
public final class Account {
  private static final Duration SUBSCRIPTION_CYCLE = Duration.ofDays(30);

  private final String id;
  private final String name;
  private final EmailAddress email;
  private String passwordHash;
  private AccountStatus status;
  private VerificationChallenge verificationChallenge;
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
      Instant subscriptionRenewsAt) {
    this.id = Objects.requireNonNull(id);
    this.name = requireText(name, "name");
    this.email = Objects.requireNonNull(email);
    this.passwordHash = requireText(passwordHash, "passwordHash");
    this.status = Objects.requireNonNull(status);
    if ((verificationCodeHash == null) != (verificationExpiresAt == null)) {
      throw new IllegalArgumentException(
          "verification hash and expiration must be present together");
    }
    this.verificationChallenge =
        verificationCodeHash == null
            ? null
            : new VerificationChallenge(verificationCodeHash, verificationExpiresAt);
    this.currentPlanCode = requireText(currentPlanCode, "currentPlanCode").toUpperCase(Locale.ROOT);
    this.subscriptionStatus = Objects.requireNonNull(subscriptionStatus);
    this.subscriptionRenewsAt = subscriptionRenewsAt;
  }

  /** Creates a pending account with a positive challenge lifetime and the essential plan. */
  public static Account register(
      String name,
      EmailAddress email,
      String passwordHash,
      String verificationCodeHash,
      Instant now,
      Duration verificationTtl) {
    var challenge = VerificationChallenge.issue(verificationCodeHash, now, verificationTtl);
    return new Account(
        UUID.randomUUID().toString(),
        name,
        email,
        passwordHash,
        AccountStatus.PENDING_VERIFICATION,
        challenge.codeHash(),
        challenge.expiresAt(),
        PlanCatalog.ESSENTIAL,
        SubscriptionStatus.ACTIVE,
        now.plus(SUBSCRIPTION_CYCLE));
  }

  /** Creates an active account only from server-verified social identity evidence. */
  public static Account registerSocial(String name, EmailAddress email, String unusablePasswordHash,
      Instant now) {
    Objects.requireNonNull(now, "now");
    return new Account(
        UUID.randomUUID().toString(), name, email, unusablePasswordHash,
        AccountStatus.ACTIVE, null, null, PlanCatalog.ESSENTIAL,
        SubscriptionStatus.ACTIVE, now.plus(SUBSCRIPTION_CYCLE));
  }

  /** Reconstructs stored account state whose subscription fields use the essential defaults. */
  public static Account rehydrate(
      String id,
      String name,
      EmailAddress email,
      String passwordHash,
      AccountStatus status,
      String verificationCodeHash,
      Instant verificationExpiresAt) {
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
        null);
  }

  /** Restores stored state without executing verification or subscription transitions. */
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
      Instant subscriptionRenewsAt) {
    String safePlan =
        currentPlanCode == null || currentPlanCode.isBlank()
            ? PlanCatalog.ESSENTIAL
            : currentPlanCode;
    SubscriptionStatus safeSubscriptionStatus =
        subscriptionStatus == null ? SubscriptionStatus.ACTIVE : subscriptionStatus;
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
        subscriptionRenewsAt);
  }

  /** Requires pending state, a stored challenge hash and an unexpired challenge. */
  public boolean canCompleteVerification(Instant now) {
    Objects.requireNonNull(now, "now");
    return status == AccountStatus.PENDING_VERIFICATION
        && verificationChallenge != null
        && verificationChallenge.isValidAt(now);
  }

  /** Enables a pending account and erases its obsolete challenge state. */
  public void completeVerification() {
    if (status != AccountStatus.PENDING_VERIFICATION) {
      throw new IllegalStateException("account is not pending verification");
    }
    status = AccountStatus.ACTIVE;
    verificationChallenge = null;
  }

  /** Replaces the pending challenge atomically after validating all replacement state. */
  public void renewVerificationCode(
      String newVerificationCodeHash, Instant now, Duration verificationTtl) {
    if (status != AccountStatus.PENDING_VERIFICATION) {
      throw new IllegalStateException("account is not pending verification");
    }
    verificationChallenge =
        VerificationChallenge.issue(newVerificationCodeHash, now, verificationTtl);
  }

  /** Allows credential authentication only after the account is active. */
  public boolean canAuthenticate() {
    return status == AccountStatus.ACTIVE;
  }

  /**
   * Replaces encoded credentials only for an active account; recovery authorization is external.
   */
  public void replacePasswordHash(String encodedPassword) {
    if (!canAuthenticate()) throw new IllegalStateException("account is not active");
    passwordHash = requireText(encodedPassword, "passwordHash");
  }

  /** Changes the active plan without extending the renewal date on an idempotent retry. */
  public boolean changeSubscription(Plan plan, Instant now) {
    Objects.requireNonNull(plan);
    Objects.requireNonNull(now);
    if (status != AccountStatus.ACTIVE) {
      throw new IllegalStateException("account is not active");
    }
    if (subscriptionStatus == SubscriptionStatus.ACTIVE && currentPlanCode.equals(plan.code())) {
      return false;
    }
    var renewal = now.plus(SUBSCRIPTION_CYCLE);
    currentPlanCode = plan.code();
    subscriptionStatus = SubscriptionStatus.ACTIVE;
    subscriptionRenewsAt = renewal;
    return true;
  }

  /** Returns the stable account identity. */
  public String id() {
    return id;
  }

  /** Returns the normalized account display name. */
  public String name() {
    return name;
  }

  /** Returns the normalized email identity. */
  public EmailAddress email() {
    return email;
  }

  /** Returns encoded credential state for authentication and persistence. */
  public String passwordHash() {
    return passwordHash;
  }

  /** Returns account activation state. */
  public AccountStatus status() {
    return status;
  }

  /** Returns the current encoded challenge, or null after verification. */
  public String verificationCodeHash() {
    return verificationChallenge == null ? null : verificationChallenge.codeHash();
  }

  /** Returns challenge expiration, or null after verification. */
  public Instant verificationExpiresAt() {
    return verificationChallenge == null ? null : verificationChallenge.expiresAt();
  }

  /** Returns the active catalog plan identifier. */
  public String currentPlanCode() {
    return currentPlanCode;
  }

  /** Returns the current subscription state. */
  public SubscriptionStatus subscriptionStatus() {
    return subscriptionStatus;
  }

  /** Returns the subscription renewal instant. */
  public Instant subscriptionRenewsAt() {
    return subscriptionRenewsAt;
  }
}
