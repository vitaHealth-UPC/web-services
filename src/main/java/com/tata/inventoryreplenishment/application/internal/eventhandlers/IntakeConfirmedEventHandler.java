package com.tata.inventoryreplenishment.application.internal.eventhandlers;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("inventoryIntakeConfirmedEventHandler")
public class IntakeConfirmedEventHandler {
    private final InventoryCommandService commands;
    public IntakeConfirmedEventHandler(InventoryCommandService commands) { this.commands = commands; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(IntakeConfirmed event) {
        commands.consumeUnit(new ConsumeUnitCommand(event.medicationId(), event.intakeId()));
    }
}
