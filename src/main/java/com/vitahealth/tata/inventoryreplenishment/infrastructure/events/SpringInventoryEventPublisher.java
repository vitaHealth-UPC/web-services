package com.vitahealth.tata.inventoryreplenishment.infrastructure.events;

import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Delivers typed inventory events synchronously so dependent failures roll back source mutations.
 */
@Component
public class SpringInventoryEventPublisher implements InventoryEventPublisher {
  private final ApplicationEventPublisher publisher;

  public SpringInventoryEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(InventoryEvent event) {
    publisher.publishEvent(event);
  }
}
