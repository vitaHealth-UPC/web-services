package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryPersistenceEntity, String> {
    Optional<InventoryPersistenceEntity> findByMedicationId(String medicationId);
    boolean existsByMedicationId(String medicationId);
}
