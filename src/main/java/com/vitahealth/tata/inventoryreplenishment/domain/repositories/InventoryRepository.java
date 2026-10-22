package com.vitahealth.tata.inventoryreplenishment.domain.repositories;

import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import java.time.Instant;
import java.util.Optional;

/** Stores inventory history and consumption evidence within application-owned transactions. */
public interface InventoryRepository {
  /** Persists current stock and append-only receipt evidence. */
  Inventory save(Inventory inventory);

  /** Reads stock without reserving it for mutation. */
  Optional<Inventory> findByMedicationId(String medicationId);

  /**
   * Reserves current stock until the caller write transaction ends. Implementations must not
   * substitute an ordinary read; cached state must be refreshed before applying mutations.
   */
  Optional<Inventory> findByMedicationIdForUpdate(String medicationId);

  /** Checks registration; uniqueness still depends on the stored medication constraint. */
  boolean existsByMedicationId(String medicationId);

  /**
   * Checks consumption evidence; callers must reserve stock and retain the unique intake
   * constraint.
   */
  boolean hasConsumed(String intakeId);

  /** Appends unique intake consumption evidence in the same transaction as the decrement. */
  void registerConsumption(String intakeId, String inventoryId, Instant consumedAt);
}
