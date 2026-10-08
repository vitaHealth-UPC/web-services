package com.tata.inventoryreplenishment.interfaces.events;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.inventoryreplenishment.application.internal.eventhandlers.IntakeConfirmedEventHandler;
import org.springframework.stereotype.Component;

@Component("inventoryIntakeConfirmedEventConsumer")
public class IntakeConfirmedEventConsumer {
    private final IntakeConfirmedEventHandler handler;
    public IntakeConfirmedEventConsumer(IntakeConfirmedEventHandler handler) { this.handler = handler; }
    public void consume(IntakeConfirmed event) { handler.handle(event); }
}
