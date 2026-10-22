package com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices;

import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryEvent;

/** Publishes typed inventory evidence synchronously within the source mutation transaction. */
public interface InventoryEventPublisher {
  /** Delivers original evidence without swallowing consumer failure. */
  void publish(InventoryEvent event);
}
