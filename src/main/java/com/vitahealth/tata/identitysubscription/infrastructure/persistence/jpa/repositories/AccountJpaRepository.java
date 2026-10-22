package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access with explicit transactional account mutation locks. */
public interface AccountJpaRepository extends JpaRepository<AccountPersistenceEntity, String> {
  Optional<AccountPersistenceEntity> findByEmail(String email);

  /** Serializes mutations of an existing account selected by identity. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AccountPersistenceEntity a where a.id = :id")
  Optional<AccountPersistenceEntity> findByIdForUpdate(@Param("id") String id);

  /** Serializes verification and challenge renewal for a normalized email. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AccountPersistenceEntity a where a.email = :email")
  Optional<AccountPersistenceEntity> findByEmailForUpdate(@Param("email") String email);
}
