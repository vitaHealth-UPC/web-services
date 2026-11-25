package com.vitahealth.tata.inventoryreplenishment.application.internal;

import com.vitahealth.tata.inventoryreplenishment.application.result.BatchResult;
import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.services.StockCoveragePolicy;
import java.time.Instant;
import java.util.List;

/** Maps stock and immutable receipt facts without changing inventory or publishing events. */
public final class InventoryMapper {
  private InventoryMapper() {}

  /** Maps evidence with unknown consumption rate and coverage. */
  public static InventoryResult toResult(Inventory inventory) {
    return toResult(inventory, null);
  }

  /** Calculates whole-day coverage from the supplied inventory units per day. */
  public static InventoryResult toResult(Inventory inventory, Integer dailyUnits) {
    return toResult(inventory, dailyUnits, List.of());
  }

  /** Adds the newest unit decrements so the caregiver sees intakes beside receipts. */
  public static InventoryResult toResult(
      Inventory inventory, Integer dailyUnits, List<Instant> recentConsumptions) {
    return new InventoryResult(
        inventory.id(),
        inventory.medicationId(),
        inventory.remainingStock(),
        inventory.replenishmentThreshold(),
        inventory.isLowStock(),
        inventory.batches().stream()
            .map(
                batch ->
                    new BatchResult(
                        batch.id(), batch.quantity(), batch.registeredAt(), batch.lot()))
            .toList(),
        inventory.createdAt(),
        inventory.updatedAt(),
        StockCoveragePolicy.daysRemaining(inventory.remainingStock(), dailyUnits),
        dailyUnits == null || dailyUnits <= 0 ? null : dailyUnits,
        inventory.latestReceiptCorrectable(),
        recentConsumptions);
  }
}
