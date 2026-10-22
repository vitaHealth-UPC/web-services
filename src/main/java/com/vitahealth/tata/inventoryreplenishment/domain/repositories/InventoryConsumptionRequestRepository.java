package com.vitahealth.tata.inventoryreplenishment.domain.repositories;

import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persists recoverable decrement intent independently of successful consumption markers. */
public interface InventoryConsumptionRequestRepository {
  /** Inserts within the source transaction; an existing identity must retain identical evidence. */
  void enqueue(PendingInventoryConsumption request);

  /** Reads pending evidence without granting a processing reservation. */
  Optional<PendingInventoryConsumption> findByIntakeId(String intakeId);

  /** Locks and refreshes pending evidence until the caller write transaction ends. */
  Optional<PendingInventoryConsumption> findByIntakeIdForUpdate(String intakeId);

  /**
   * Reads at most limit due snapshots in next-attempt/identity order, after an optional exclusive
   * snapshot cursor. Implementations must bound limit to 1..100 and never lock an entire batch.
   */
  List<PendingInventoryConsumption> findDueAfter(
      Instant cutoff, PendingInventoryConsumption after, int limit);

  /** Deletes a reserved request in the same transaction as its successful decrement marker. */
  void complete(String intakeId);

  /**
   * Advances only retry metadata if the reserved persisted attempt still matches expected. A
   * concurrently completed request must not be recreated by failure handling.
   */
  void recordRetry(PendingInventoryConsumption expected, Instant nextAttemptAt);
}
