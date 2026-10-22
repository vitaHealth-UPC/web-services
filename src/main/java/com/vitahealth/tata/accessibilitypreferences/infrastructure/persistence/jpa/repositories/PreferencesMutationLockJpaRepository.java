package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.PreferencesMutationLockEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Creates unique user coordination keys and reserves them across each caller's decision. */
public interface PreferencesMutationLockJpaRepository
    extends JpaRepository<PreferencesMutationLockEntity, String> {
  /** Checks whether a technical user key exists, independently of preference state. */
  Optional<PreferencesMutationLockEntity> findByUserId(String userId);

  /** Reserves this logical user even when no preference row exists yet. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select reservation from PreferencesMutationLockEntity reservation where reservation.userId ="
          + " :userId")
  Optional<PreferencesMutationLockEntity> findForUpdate(@Param("userId") String userId);
}
