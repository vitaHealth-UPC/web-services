package com.tata.inventoryreplenishment.infrastructure.events;

import com.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes LowStockDetected and ReplenishmentRegistered as in-process Spring Application Events. */
@Component
public class SpringInventoryEventPublisher implements InventoryEventPublisher {
    private final ApplicationEventPublisher publisher;

    public SpringInventoryEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
