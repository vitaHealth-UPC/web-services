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
        Instant updatedAt
) {
    public InventoryResult {
        batches = batches == null ? List.of() : List.copyOf(batches);
    }
}
