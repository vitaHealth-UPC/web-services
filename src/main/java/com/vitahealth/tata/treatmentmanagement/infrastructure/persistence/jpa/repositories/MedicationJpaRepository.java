package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.MedicationPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides context-owned medication reads and explicit transactional row locking. */
public interface MedicationJpaRepository
    extends JpaRepository<MedicationPersistenceEntity, String> {
  /** Acquires the aggregate row lock before a lifecycle mutation. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select evidence from MedicationPersistenceEntity evidence where evidence.id = :id")
  Optional<MedicationPersistenceEntity> findForUpdate(@Param("id") String id);

  List<MedicationPersistenceEntity> findByOlderAdultIdOrderByNameAsc(String olderAdultId);

  /** Reads bounded source references without materializing medication aggregates. */
  @Query(
      "select medication.id from MedicationPersistenceEntity medication where"
          + " medication.olderAdultId = :adultId and medication.id > :afterId order by"
          + " medication.id")
  List<String> findIdsAfter(
      @Param("adultId") String adultId, @Param("afterId") String afterId, Pageable page);
}
