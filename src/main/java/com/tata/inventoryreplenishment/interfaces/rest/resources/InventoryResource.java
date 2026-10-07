package com.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Stock of a medication")
public record InventoryResource(
        String id,
        String medicationId,
        @Schema(example = "12") int remainingStock,
        @Schema(example = "5") int replenishmentThreshold,
        @Schema(description = "True when the remaining stock reaches or falls below the threshold (US-42)")
        boolean lowStock,
        List<BatchResource> batches,
        Instant createdAt,
        Instant updatedAt,
        Integer daysRemaining,
        Integer dailyConsumptionUnits
) {
    public InventoryResource(String id, String medicationId, int remainingStock, int replenishmentThreshold, boolean lowStock,
            List<BatchResource> batches, Instant createdAt, Instant updatedAt) {
        this(id,medicationId,remainingStock,replenishmentThreshold,lowStock,batches,createdAt,updatedAt,null,null);
    }

}
