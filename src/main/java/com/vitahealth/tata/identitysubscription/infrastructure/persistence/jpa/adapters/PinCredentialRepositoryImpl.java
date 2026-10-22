package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicatePinCredentialException;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.PinCredentialPersistenceAssembler;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PinCredentialJpaRepository;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapts PIN persistence while retaining failed-attempt locks in the authentication transaction.
 */
@Repository
public class PinCredentialRepositoryImpl implements PinCredentialRepository {
  private final PinCredentialJpaRepository repository;

  /** Injects the persistence gateway for the PIN aggregate. */
  public PinCredentialRepositoryImpl(PinCredentialJpaRepository repository) {
    this.repository = repository;
  }

  /** Reads credential state without acquiring the authentication mutation lock. */
  @Override
  public Optional<PinCredential> findByOlderAdultId(String olderAdultId) {
    return repository
        .findByOlderAdultId(olderAdultId)
        .map(PinCredentialPersistenceAssembler::toDomain);
  }

  /** Requires the authentication transaction so the lock covers counter update and validation. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<PinCredential> findForAuthentication(String olderAdultId) {
    return repository
        .findForAuthentication(olderAdultId)
        .map(PinCredentialPersistenceAssembler::toDomain);
  }

  /** Persists encoded PIN and failed-attempt state without plaintext credentials. */
  @Override
  @Transactional
  public PinCredential save(PinCredential credential) {
    try {
      var state = PinCredentialPersistenceAssembler.toEntity(credential);
      var existing = repository.findByIdForUpdate(credential.id());
      if (existing.isEmpty()) {
        return PinCredentialPersistenceAssembler.toDomain(repository.saveAndFlush(state));
      }
      var managed = existing.orElseThrow();
      managed.updateFrom(state);
      repository.flush();
      return PinCredentialPersistenceAssembler.toDomain(managed);
    } catch (DataIntegrityViolationException exception) {
      if (UniqueConstraintClassifier.matches(exception, "uk_identity_pin_adult")) {
        throw new DuplicatePinCredentialException();
      }
      throw exception;
    }
  }
}
