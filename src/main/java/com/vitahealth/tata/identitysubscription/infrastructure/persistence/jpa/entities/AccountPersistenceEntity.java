package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

/** Stores account state independently of domain transitions and technical audit timestamps. */
@Entity
@Table(
    name = "identity_accounts",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_identity_accounts_email", columnNames = "email"))
public class AccountPersistenceEntity extends AuditableAbstractPersistenceEntity {
  @Id private String id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 200)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AccountStatus status;

  @Column(name = "verification_code_hash", length = 100)
  private String verificationCodeHash;

  @Column(name = "verification_expires_at")
  private Instant verificationExpiresAt;

  @Column(name = "subscription_plan_code", length = 40)
  private String subscriptionPlanCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "subscription_status", length = 40)
  private SubscriptionStatus subscriptionStatus;

  @Column(name = "subscription_renews_at")
  private Instant subscriptionRenewsAt;

  /** Constructor required by JPA hydration. */
  protected AccountPersistenceEntity() {}

  /** Creates persistence state using the account identity supplied by the domain. */
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
      Instant subscriptionRenewsAt) {
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

  /** Updates managed account state without replacing its identity or persistence audit. */
  public void updateFrom(AccountPersistenceEntity state) {
    if (!Objects.equals(id, state.id)) {
      throw new IllegalArgumentException("Cannot replace account persistence identity");
    }
    name = state.name;
    email = state.email;
    passwordHash = state.passwordHash;
    status = state.status;
    verificationCodeHash = state.verificationCodeHash;
    verificationExpiresAt = state.verificationExpiresAt;
    subscriptionPlanCode = state.subscriptionPlanCode;
    subscriptionStatus = state.subscriptionStatus;
    subscriptionRenewsAt = state.subscriptionRenewsAt;
  }

  /** Returns the stored account identity. */
  public String getId() {
    return id;
  }

  /** Returns the account display name. */
  public String getName() {
    return name;
  }

  /** Returns the normalized account email. */
  public String getEmail() {
    return email;
  }

  /** Returns the password hash for domain reconstruction only. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Returns the account verification state. */
  public AccountStatus getStatus() {
    return status;
  }

  /** Returns the current challenge hash for verification and delivery eligibility. */
  public String getVerificationCodeHash() {
    return verificationCodeHash;
  }

  /** Returns the domain challenge expiration. */
  public Instant getVerificationExpiresAt() {
    return verificationExpiresAt;
  }

  /** Returns the current catalog plan identifier. */
  public String getSubscriptionPlanCode() {
    return subscriptionPlanCode;
  }

  /** Returns the subscription state. */
  public SubscriptionStatus getSubscriptionStatus() {
    return subscriptionStatus;
  }

  /** Returns the domain subscription renewal time. */
  public Instant getSubscriptionRenewsAt() {
    return subscriptionRenewsAt;
  }
}
