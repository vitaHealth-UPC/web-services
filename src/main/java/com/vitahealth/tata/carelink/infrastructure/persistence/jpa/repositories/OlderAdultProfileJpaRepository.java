package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.OlderAdultProfilePersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Loads adult profile rows and reserves them for serialized command updates. */
public interface OlderAdultProfileJpaRepository
    extends JpaRepository<OlderAdultProfilePersistenceEntity, String> {
  /** Reserves the row in the caller's transaction before changing basic data or contact. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select profile from OlderAdultProfilePersistenceEntity profile where profile.id = :id")
  Optional<OlderAdultProfilePersistenceEntity> findByIdForUpdate(@Param("id") String id);
}
