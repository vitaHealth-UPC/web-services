package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyStockCatchUpPersistenceEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads bounded eligible work without locking a complete recovery batch. */
public interface FamilyStockCatchUpJpaRepository
    extends JpaRepository<FamilyStockCatchUpPersistenceEntity, Long> {
  /** Continues a deterministic scan past the supplied monitor identity. */
  @Query(
      "select work from FamilyStockCatchUpPersistenceEntity work where work.nextAttemptAt <="
          + " :cutoff and work.monitorId > :afterId order by work.monitorId")
  List<FamilyStockCatchUpPersistenceEntity> findDueAfter(
      @Param("cutoff") Instant cutoff, @Param("afterId") long afterId, Pageable page);
}
