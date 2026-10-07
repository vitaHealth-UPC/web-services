package com.tata.inventoryreplenishment.application.internal;

import com.tata.inventoryreplenishment.application.models.BatchResult;
import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;

public final class InventoryMapper {
    private InventoryMapper() {}

    public static InventoryResult toResult(Inventory inventory) {
        return new InventoryResult(
                inventory.id(),
                inventory.medicationId(),
                inventory.remainingStock(),
                inventory.replenishmentThreshold(),
                inventory.isLowStock(),
                inventory.batches().stream()
                        .map(batch -> new BatchResult(batch.id(), batch.quantity(), batch.registeredAt()))
                        .toList(),
                inventory.createdAt(),
                inventory.updatedAt()
        );
    }
}
