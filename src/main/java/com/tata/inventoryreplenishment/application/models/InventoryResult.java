package com.tata.inventoryreplenishment.application.models;

import java.time.Instant;
import java.util.List;

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
        Integer dailyConsumptionUnits
) {
    public InventoryResult(String id, String medicationId, int remainingStock, int replenishmentThreshold, boolean lowStock,
            List<BatchResult> batches, Instant createdAt, Instant updatedAt) {
        this(id,medicationId,remainingStock,replenishmentThreshold,lowStock,batches,createdAt,updatedAt,null,null);
    }

    public InventoryResult {
        batches = batches == null ? List.of() : List.copyOf(batches);
    }
}
