package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Public stock evidence with immutable receipts and optional calculated coverage. */
@Schema(description = "Stock of a medication")
public record InventoryResource(
    String id,
    String medicationId,
    @Schema(example = "12") int remainingStock,
    @Schema(example = "5") int replenishmentThreshold,
    @Schema(
            description =
                "True when the remaining stock reaches or falls below the threshold (US-42)")
        boolean lowStock,
    List<BatchResource> batches,
    Instant createdAt,
    Instant updatedAt,
    @Schema(description = "Whole days of coverage; null when the daily consumption rate is unknown")
        Integer daysRemaining,
    @Schema(description = "Inventory units per day; null when no reliable schedule is available")
        Integer dailyConsumptionUnits) {
  /** Copies receipt history so a response cannot change through an external mutable list. */
  public InventoryResource {
    batches = List.copyOf(batches);
  }
}
