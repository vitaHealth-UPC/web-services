package com.vitahealth.tata.identitysubscription.domain.repositories;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import java.util.Optional;

/** Loads account aggregates and persists their identity and verification/subscription state. */
public interface AccountRepository {
  /** Returns the current account state without requesting a mutation lock. */
  Optional<Account> findById(String id);

  /** Returns the account identified by an already normalized email address. */
  Optional<Account> findByEmail(String email);

  /** Locks an existing account until the caller's transaction ends; requires a transaction. */
  Optional<Account> findByIdForUpdate(String id);

  /**
   * Locks the email-selected account until the caller's transaction ends; requires a transaction.
   */
  Optional<Account> findByEmailForUpdate(String email);

  /** Persists domain state while retaining existing identity and technical persistence metadata. */
  Account save(Account account);
}
