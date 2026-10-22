package com.vitahealth.tata.familymonitoring.interfaces.events;

import com.vitahealth.tata.familymonitoring.application.internal.commandservices.ProjectInventoryStockCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.commands.ProjectInventoryStockCommand;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryStockChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Translates complete inventory evidence into the local revision-aware projection command. */
@Component
public class InventoryStockChangedEventConsumer {
  private final ProjectInventoryStockCommandHandler handler;

  /** Injects the local application boundary without accessing foreign persistence. */
  public InventoryStockChangedEventConsumer(ProjectInventoryStockCommandHandler handler) {
    this.handler = handler;
  }

  /** Preserves all source evidence and synchronous rollback semantics. */
  @EventListener
  public void consume(InventoryStockChanged event) {
    handler.handle(
        new ProjectInventoryStockCommand(
            new InventoryStockPosition(
                event.medicationId(),
                event.inventoryId(),
                event.revision(),
                event.remainingStock(),
                event.replenishmentThreshold(),
                event.changedAt())));
  }
}
