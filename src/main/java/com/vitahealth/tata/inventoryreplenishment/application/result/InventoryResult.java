package com.vitahealth.tata.inventoryreplenishment.application.result;

import java.time.Instant;
import java.util.List;

/** Immutable inventory evidence; null coverage indicates an unknown consumption rate. */
public record InventoryResult(
    String id,
    String medicationId,
    int remainingStock,
    int replenishmentThreshold,
    boolean lowStock,
    List<BatchResult> batches,
    Instant createdAt,
    Instant updatedAt,
    Integer daysRemaining,
    Integer dailyConsumptionUnits) {
  /** Builds evidence without optional coverage or lot information. */
  public InventoryResult(
      String id,
      String medicationId,
      int remainingStock,
      int replenishmentThreshold,
      boolean lowStock,
      List<BatchResult> batches,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        medicationId,
        remainingStock,
        replenishmentThreshold,
        lowStock,
        batches,
        createdAt,
        updatedAt,
        null,
        null);
  }

  /** Copies receipt evidence so response history cannot follow caller list mutations. */
  public InventoryResult {
    batches = batches == null ? List.of() : List.copyOf(batches);
  }
}
