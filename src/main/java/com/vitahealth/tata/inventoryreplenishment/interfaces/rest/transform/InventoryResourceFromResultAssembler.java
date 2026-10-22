package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.transform;

import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources.BatchResource;
import com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources.InventoryResource;

/**
 * Maps application stock evidence into the public response without recalculating business facts.
 */
public final class InventoryResourceFromResultAssembler {
  private InventoryResourceFromResultAssembler() {}

  /** Preserves receipt history, nullable coverage and source audit timestamps. */
  public static InventoryResource fromResult(InventoryResult result) {
    return new InventoryResource(
        result.id(),
        result.medicationId(),
        result.remainingStock(),
        result.replenishmentThreshold(),
        result.lowStock(),
        result.batches().stream()
            .map(
                batch ->
                    new BatchResource(
                        batch.id(), batch.quantity(), batch.registeredAt(), batch.lot()))
            .toList(),
        result.createdAt(),
        result.updatedAt(),
        result.daysRemaining(),
        result.dailyConsumptionUnits());
  }
}
