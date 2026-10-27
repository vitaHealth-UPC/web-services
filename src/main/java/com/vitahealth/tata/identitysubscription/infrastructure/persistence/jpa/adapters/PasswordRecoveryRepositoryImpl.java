package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PasswordRecoveryChallenge;
import com.vitahealth.tata.identitysubscription.domain.repositories.PasswordRecoveryRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.PasswordRecoveryPersistenceAssembler;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.*;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

/** Account-first locking serializes issuance and use, including absent recovery rows. */
@Repository
public class PasswordRecoveryRepositoryImpl implements PasswordRecoveryRepository {
  private final AccountJpaRepository accounts;
  private final PasswordRecoveryJpaRepository recoveries;

  public PasswordRecoveryRepositoryImpl(
      AccountJpaRepository accounts, PasswordRecoveryJpaRepository recoveries) {
    this.accounts = accounts;
    this.recoveries = recoveries;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PasswordRecoveryChallenge> findByAccountId(String id) {
    return recoveries.findById(id).map(PasswordRecoveryPersistenceAssembler::toDomain);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<PasswordRecoveryChallenge> findByAccountIdForUpdate(String id) {
    if (accounts.findByIdForUpdate(id).isEmpty()) return Optional.empty();
    return recoveries.findForUpdate(id).map(PasswordRecoveryPersistenceAssembler::toDomain);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void save(String id, PasswordRecoveryChallenge challenge) {
    if (accounts.findByIdForUpdate(id).isEmpty())
      throw new IllegalArgumentException("Recovery account not found");
    var state = PasswordRecoveryPersistenceAssembler.toEntity(id, challenge);
    var current = recoveries.findForUpdate(id);
    if (current.isPresent()) current.get().updateFrom(state);
    else recoveries.saveAndFlush(state);
  }
}
