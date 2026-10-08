package com.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.AccountPersistenceAssembler;
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
        return repository.findById(id).map(AccountPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email).map(AccountPersistenceAssembler::toDomain);
    }

    @Override
    public Account save(Account account) {
        return AccountPersistenceAssembler.toDomain(repository.save(AccountPersistenceAssembler.toEntity(account)));
    }

}
