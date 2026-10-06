package com.tata.inventoryreplenishment.domain.repositories;

import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;

import java.time.Instant;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findByMedicationId(String medicationId);
    default Optional<Inventory> findByMedicationIdForUpdate(String medicationId) { return findByMedicationId(medicationId); }
    boolean existsByMedicationId(String medicationId);

    /** Whether the unit for this intake was already consumed; guarantees one decrement per intake. */
    boolean hasConsumed(String intakeId);
    void registerConsumption(String intakeId, String inventoryId, Instant consumedAt);
}
