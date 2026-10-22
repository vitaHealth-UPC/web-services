package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakeScheduleLockEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Owns reservation-key creation checks and transaction-held database row locks. */
public interface IntakeScheduleLockJpaRepository
    extends JpaRepository<IntakeScheduleLockEntity, String> {
  /** Checks whether the technical key has already been initialized. */
  Optional<IntakeScheduleLockEntity> findByTreatmentId(String treatmentId);

  /** Reserves the treatment independently of whether any intake rows have been generated. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select reservation from IntakeScheduleLockEntity reservation where reservation.treatmentId ="
          + " :treatmentId")
  Optional<IntakeScheduleLockEntity> findForUpdate(@Param("treatmentId") String treatmentId);
}
