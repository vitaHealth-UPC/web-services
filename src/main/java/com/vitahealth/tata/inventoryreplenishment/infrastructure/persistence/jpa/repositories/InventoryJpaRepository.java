package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Retrieves medication-scoped managed stock; the adapter owns explicit refresh reservations. */
public interface InventoryJpaRepository extends JpaRepository<InventoryPersistenceEntity, String> {
  /** Reads stock without requesting a mutation reservation. */
  Optional<InventoryPersistenceEntity> findByMedicationId(String medicationId);

  /** Checks the unique medication registration key. */
  boolean existsByMedicationId(String medicationId);
}
