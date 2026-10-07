package com.tata.inventoryreplenishment.application.internal.outboundservices;

/** Publishes LowStockDetected and ReplenishmentRegistered to other Bounded Contexts. */
public interface InventoryEventPublisher {
    void publish(Object event);
}
