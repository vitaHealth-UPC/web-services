package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicateAccountEmailException;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.AccountPersistenceAssembler;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountJpaRepository;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps account persistence to domain without leaking JPA or weakening mutation locks. */
@Repository
public class AccountRepositoryImpl implements AccountRepository {
  private final AccountJpaRepository repository;

  /** Injects the Spring Data gateway without exposing its entities to application callers. */
  public AccountRepositoryImpl(AccountJpaRepository repository) {
    this.repository = repository;
  }

  /** Loads account state without requesting a mutation lock. */
  @Override
  public Optional<Account> findById(String id) {
    return repository.findById(id).map(AccountPersistenceAssembler::toDomain);
  }

  /** Loads an account by its normalized email address. */
  @Override
  public Optional<Account> findByEmail(String email) {
    return repository.findByEmail(email).map(AccountPersistenceAssembler::toDomain);
  }

  /** Retains the account lock in the caller's command transaction. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Account> findByIdForUpdate(String id) {
    return repository.findByIdForUpdate(id).map(AccountPersistenceAssembler::toDomain);
  }

  /** Retains the email-selected account lock through verification or renewal. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<Account> findByEmailForUpdate(String email) {
    return repository.findByEmailForUpdate(email).map(AccountPersistenceAssembler::toDomain);
  }

  /** Saves new state or updates the existing managed entity while preserving technical audit. */
  @Override
  @Transactional
  public Account save(Account account) {
    var state = AccountPersistenceAssembler.toEntity(account);
    var existing = repository.findByIdForUpdate(account.id());
    if (existing.isEmpty()) {
      try {
        return AccountPersistenceAssembler.toDomain(repository.saveAndFlush(state));
      } catch (DataIntegrityViolationException exception) {
        if (UniqueConstraintClassifier.matches(exception, "uk_identity_accounts_email")) {
          throw new DuplicateAccountEmailException();
        }
        throw exception;
      }
    }
    var managed = existing.get();
    managed.updateFrom(state);
    return AccountPersistenceAssembler.toDomain(managed);
  }
}
