package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionRequestPersistenceEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads bounded recovery pages without locking whole batches. */
public interface InventoryConsumptionRequestJpaRepository
    extends JpaRepository<InventoryConsumptionRequestPersistenceEntity, String> {
  /** Reads the initial due page in deterministic time and identity order. */
  @Query(
      "select r from InventoryConsumptionRequestPersistenceEntity r where r.nextAttemptAt <="
          + " :cutoff order by r.nextAttemptAt, r.intakeId")
  List<InventoryConsumptionRequestPersistenceEntity> findDue(
      @Param("cutoff") Instant cutoff, Pageable page);

  /** Continues after the complete exclusive position, without nullable SQL cursor parameters. */
  @Query(
      "select r from InventoryConsumptionRequestPersistenceEntity r where r.nextAttemptAt <="
          + " :cutoff and (r.nextAttemptAt > :afterAt or (r.nextAttemptAt = :afterAt and r.intakeId"
          + " > :afterId)) order by r.nextAttemptAt, r.intakeId")
  List<InventoryConsumptionRequestPersistenceEntity> findDueAfter(
      @Param("cutoff") Instant cutoff,
      @Param("afterAt") Instant afterAt,
      @Param("afterId") String afterId,
      Pageable page);
}
