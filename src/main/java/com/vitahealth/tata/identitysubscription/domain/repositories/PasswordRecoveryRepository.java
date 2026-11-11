package com.vitahealth.tata.identitysubscription.domain.repositories;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PasswordRecoveryChallenge;
import java.util.Optional;

/** Recovery state is separate from account-verification evidence. */
public interface PasswordRecoveryRepository {
  Optional<PasswordRecoveryChallenge> findByAccountId(String accountId);

  /**
   * Requires a caller transaction and reserves the account even before a first challenge exists.
   */
  Optional<PasswordRecoveryChallenge> findByAccountIdForUpdate(String accountId);

  /**
   * Participates in the caller transaction so failed attempts or consumption cannot commit alone.
   */
  void save(String accountId, PasswordRecoveryChallenge challenge);
}
