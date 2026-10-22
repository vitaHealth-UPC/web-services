package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.VerificationDeliveryPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Retrieves a bounded batch of committed delivery intentions ready for retry. */
public interface VerificationDeliveryJpaRepository
    extends JpaRepository<VerificationDeliveryPersistenceEntity, String> {
  /** Orders due requests deterministically and excludes erased terminal payloads. */
  List<VerificationDeliveryPersistenceEntity>
      findTop20ByCompletedFalseAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
          Instant now);

  /** Serializes reservation and completion across application instances. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from VerificationDeliveryPersistenceEntity d where d.id = :id")
  Optional<VerificationDeliveryPersistenceEntity> findForUpdate(@Param("id") String id);
}
