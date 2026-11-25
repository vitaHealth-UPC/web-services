package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionPersistenceEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persists unique intake markers; inventory reservations coordinate concurrent consumption. */
public interface InventoryConsumptionJpaRepository
    extends JpaRepository<InventoryConsumptionPersistenceEntity, Long> {
  /** Checks committed or current-transaction markers without replacing the unique constraint. */
  boolean existsByIntakeId(String intakeId);

  /** Reads the newest decrement markers of one inventory. */
  List<InventoryConsumptionPersistenceEntity> findByInventoryIdOrderByConsumedAtDesc(
      String inventoryId, Pageable page);
}
