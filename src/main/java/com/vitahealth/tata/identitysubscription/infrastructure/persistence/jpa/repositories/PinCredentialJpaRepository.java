package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Stores PIN credentials and serializes authentication attempts for each older adult. */
public interface PinCredentialJpaRepository
    extends JpaRepository<PinCredentialPersistenceEntity, String> {
  /** Finds the credential without reserving it for an authentication update. */
  Optional<PinCredentialPersistenceEntity> findByOlderAdultId(String olderAdultId);

  /** Reserves an existing credential while applying a domain snapshot in the caller transaction. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PinCredentialPersistenceEntity p where p.id = :id")
  Optional<PinCredentialPersistenceEntity> findByIdForUpdate(@Param("id") String id);

  /** Locks the credential so concurrent attempts cannot lose failure counts or lockout state. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PinCredentialPersistenceEntity p where p.olderAdultId = :olderAdultId")
  Optional<PinCredentialPersistenceEntity> findForAuthentication(
      @Param("olderAdultId") String olderAdultId);
}
