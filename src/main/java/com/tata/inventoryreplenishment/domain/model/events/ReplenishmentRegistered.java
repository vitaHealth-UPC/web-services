package com.tata.inventoryreplenishment.domain.model.events;

import java.time.Instant;

/** Published by Inventory & Replenishment when a replenishment batch is registered (US-43). */
public record ReplenishmentRegistered(
        String inventoryId,
        String medicationId,
        String batchId,
        int quantity,
        int remainingStock,
        Instant registeredAt
) {
}
