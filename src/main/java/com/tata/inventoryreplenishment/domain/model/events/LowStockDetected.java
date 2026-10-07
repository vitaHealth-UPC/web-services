package com.tata.inventoryreplenishment.domain.model.events;

import java.time.Instant;

/** Published by Inventory & Replenishment when the stock of a medication crosses its threshold (US-42). */
public record LowStockDetected(
        String inventoryId,
        String medicationId,
        int remainingStock,
        int replenishmentThreshold,
        Instant detectedAt
) {
}
