package com.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;

public final class AccountPersistenceAssembler {
    private AccountPersistenceAssembler() {}

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
                entity.getSubscriptionRenewsAt()
        );
    }

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
                account.subscriptionRenewsAt()
        );
    }
}
