package com.tata.familymonitoring.application.internal.eventhandlers;

import com.tata.familymonitoring.application.internal.commandservices.ResolveLowStockCommandHandler;
import com.tata.familymonitoring.domain.model.commands.ResolveLowStockCommand;
import com.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ReplenishmentRegisteredEventHandler {
  private final ResolveLowStockCommandHandler resolveLowStock;

  public ReplenishmentRegisteredEventHandler(ResolveLowStockCommandHandler resolveLowStock) {
    this.resolveLowStock = resolveLowStock;
  }

  @EventListener
  public void handle(ReplenishmentRegistered event) {
    resolveLowStock.handle(new ResolveLowStockCommand(event.medicationId(), event.remainingStock()));
  }
}
