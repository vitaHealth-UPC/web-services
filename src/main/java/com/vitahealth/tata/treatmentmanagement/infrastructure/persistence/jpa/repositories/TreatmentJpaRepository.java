package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides context-owned treatment reads and explicit transactional row locking. */
public interface TreatmentJpaRepository extends JpaRepository<TreatmentPersistenceEntity, String> {
  /** Acquires the aggregate row lock before a lifecycle mutation. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select evidence from TreatmentPersistenceEntity evidence where evidence.id = :id")
  Optional<TreatmentPersistenceEntity> findForUpdate(@Param("id") String id);

  /** Lists the adult owner's treatments by their original registration time. */
  List<TreatmentPersistenceEntity> findByOlderAdultIdOrderByCreatedAtAsc(String olderAdultId);

  /** Finds dependent treatments before locking them individually for medication changes. */
  List<TreatmentPersistenceEntity> findByMedicationId(String medicationId);

  /** Returns lifecycle-selected evidence for context-owned queries. */
  List<TreatmentPersistenceEntity> findByStatus(TreatmentStatus status);

  /** Selects only identifiers with keyset pagination so changing status does not shift offsets. */
  @Query(
      "select evidence.id from TreatmentPersistenceEntity evidence where evidence.status = :status"
          + " and evidence.id > :afterId order by evidence.id")
  List<String> findIdsByStatusAfter(
      @Param("status") TreatmentStatus status, @Param("afterId") String afterId, Pageable page);
}
