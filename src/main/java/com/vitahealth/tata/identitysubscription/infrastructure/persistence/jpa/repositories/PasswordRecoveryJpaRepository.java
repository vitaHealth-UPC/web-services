package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PasswordRecoveryJpaRepository
    extends JpaRepository<PasswordRecoveryPersistenceEntity, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from PasswordRecoveryPersistenceEntity r where r.accountId = :accountId")
  Optional<PasswordRecoveryPersistenceEntity> findForUpdate(@Param("accountId") String accountId);
}
