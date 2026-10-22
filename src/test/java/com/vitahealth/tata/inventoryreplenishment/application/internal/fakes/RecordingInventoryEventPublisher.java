package com.vitahealth.tata.inventoryreplenishment.application.internal.fakes;

import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecordingInventoryEventPublisher implements InventoryEventPublisher {
  public final List<InventoryEvent> events = new ArrayList<>();

  @Override
  public void publish(InventoryEvent event) {
    events.add(event);
  }
}
