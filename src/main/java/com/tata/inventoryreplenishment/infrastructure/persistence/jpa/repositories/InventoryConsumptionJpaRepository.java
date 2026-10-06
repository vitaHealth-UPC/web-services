package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryConsumptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryConsumptionJpaRepository extends JpaRepository<InventoryConsumptionPersistenceEntity, Long> {
    boolean existsByIntakeId(String intakeId);
}
