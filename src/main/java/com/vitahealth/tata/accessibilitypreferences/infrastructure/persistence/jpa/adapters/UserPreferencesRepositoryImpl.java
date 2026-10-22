package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.assemblers.UserPreferencesPersistenceAssembler;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories.UserPreferencesJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps complete preference snapshots and refreshes row-locked state before mutation. */
@Repository
public class UserPreferencesRepositoryImpl implements UserPreferencesRepository {
  private final UserPreferencesJpaRepository repository;
  private final EntityManager entities;
  private final Clock clock;

  /**
   * Injects storage, fresh locked reads, and the common clock used for deliberate write evidence.
   */
  public UserPreferencesRepositoryImpl(
      UserPreferencesJpaRepository repository, EntityManager entities, Clock clock) {
    this.repository = repository;
    this.entities = entities;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Persists the complete decision through the current repository transaction. */
  @Override
  @Transactional
  public UserPreferences save(UserPreferences preferences) {
    var replacement = UserPreferencesPersistenceAssembler.toEntity(preferences);
    var existing = repository.findForUpdate(preferences.userId());
    if (existing.isEmpty()) {
      replacement.recordWrite(clock.instant());
      entities.persist(replacement);
      entities.flush();
      return UserPreferencesPersistenceAssembler.toDomain(replacement);
    }
    var managed = existing.orElseThrow();
    entities.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
    if (!managed.getId().equals(preferences.id()))
      throw new IllegalStateException("preference identity does not match the stored owner");
    managed.applyMutableState(replacement);
    managed.recordWrite(clock.instant());
    entities.flush();
    return UserPreferencesPersistenceAssembler.toDomain(managed);
  }

  /**
   * Reconstructs complete domain state inside a read-only transaction, including lazy collections.
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<UserPreferences> findByUserId(String userId) {
    return repository.findByUserId(userId).map(UserPreferencesPersistenceAssembler::toDomain);
  }

  /** Refreshes even previously managed state after acquiring the user's preference row lock. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<UserPreferences> findByUserIdForUpdate(String userId) {
    return repository
        .findForUpdate(userId)
        .map(
            entity -> {
              entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
              return UserPreferencesPersistenceAssembler.toDomain(entity);
            });
  }
}
