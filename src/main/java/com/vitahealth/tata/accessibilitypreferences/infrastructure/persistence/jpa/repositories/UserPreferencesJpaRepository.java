package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Stores preference snapshots and exposes an explicit row reservation for mutations. */
public interface UserPreferencesJpaRepository
    extends JpaRepository<UserPreferencesPersistenceEntity, String> {
  /** Reads the saved preference state of the logical user. */
  Optional<UserPreferencesPersistenceEntity> findByUserId(String userId);

  /** Locks an existing preference row for the duration of its caller transaction. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select preferences from UserPreferencesPersistenceEntity preferences where"
          + " preferences.userId = :userId")
  Optional<UserPreferencesPersistenceEntity> findForUpdate(@Param("userId") String userId);
}
