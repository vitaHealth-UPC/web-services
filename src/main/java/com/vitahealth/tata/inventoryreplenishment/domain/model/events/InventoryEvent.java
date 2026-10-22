package com.vitahealth.tata.inventoryreplenishment.domain.model.events;

/** Closed family of inventory business facts and revisioned stock evidence. */
public sealed interface InventoryEvent
    permits LowStockDetected, ReplenishmentRegistered, InventoryStockChanged {}
