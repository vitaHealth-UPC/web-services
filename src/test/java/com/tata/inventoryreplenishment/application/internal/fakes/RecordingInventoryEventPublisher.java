package com.tata.inventoryreplenishment.application.internal.fakes;

import com.tata.inventoryreplenishment.application.internal.outboundservices.InventoryEventPublisher;

import java.util.ArrayList;
import java.util.List;

public final class RecordingInventoryEventPublisher implements InventoryEventPublisher {
    public final List<Object> events = new ArrayList<>();

    @Override
    public void publish(Object event) {
        events.add(event);
    }
}
