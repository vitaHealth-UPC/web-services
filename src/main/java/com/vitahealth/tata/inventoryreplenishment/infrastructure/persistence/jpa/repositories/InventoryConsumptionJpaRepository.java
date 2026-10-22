package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persists unique intake markers; inventory reservations coordinate concurrent consumption. */
public interface InventoryConsumptionJpaRepository
    extends JpaRepository<InventoryConsumptionPersistenceEntity, Long> {
  /** Checks committed or current-transaction markers without replacing the unique constraint. */
  boolean existsByIntakeId(String intakeId);
}
