package com.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryPersistenceEntity, String> {
    Optional<InventoryPersistenceEntity> findByMedicationId(String medicationId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from InventoryPersistenceEntity i where i.medicationId = :medicationId")
    Optional<InventoryPersistenceEntity> findByMedicationIdForUpdate(@org.springframework.data.repository.query.Param("medicationId") String medicationId);
    boolean existsByMedicationId(String medicationId);
}
