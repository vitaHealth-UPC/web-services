package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;

/** Converts Account state between domain and JPA representations without database access. */
public final class AccountPersistenceAssembler {
  private AccountPersistenceAssembler() {}

  /**
   * Restores the stored identity and state without executing a business transition.
   *
   * @param entity stored JPA representation
   * @return reconstructed domain object
   */
  public static Account toDomain(AccountPersistenceEntity entity) {
    return Account.rehydrate(
        entity.getId(),
        entity.getName(),
        new EmailAddress(entity.getEmail()),
        entity.getPasswordHash(),
        entity.getStatus(),
        entity.getVerificationCodeHash(),
        entity.getVerificationExpiresAt(),
        entity.getSubscriptionPlanCode(),
        entity.getSubscriptionStatus(),
        entity.getSubscriptionRenewsAt());
  }

  /**
   * Builds the persistence representation while preserving the domain identity.
   *
   * @param account account state with encoded credentials and subscription information
   * @return JPA state ready for the repository adapter
   */
  public static AccountPersistenceEntity toEntity(Account account) {
    return new AccountPersistenceEntity(
        account.id(),
        account.name(),
        account.email().value(),
        account.passwordHash(),
        account.status(),
        account.verificationCodeHash(),
        account.verificationExpiresAt(),
        account.currentPlanCode(),
        account.subscriptionStatus(),
        account.subscriptionRenewsAt());
  }
}
