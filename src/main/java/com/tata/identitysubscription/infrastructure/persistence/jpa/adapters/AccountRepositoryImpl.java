package com.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository repository;

    public AccountRepositoryImpl(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        return toDomain(repository.save(toEntity(account)));
    }

    private Account toDomain(AccountPersistenceEntity entity) {
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

    private AccountPersistenceEntity toEntity(Account account) {
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
