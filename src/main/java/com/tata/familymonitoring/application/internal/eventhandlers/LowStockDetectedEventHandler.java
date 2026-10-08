package com.tata.familymonitoring.application.internal.eventhandlers;

import com.tata.familymonitoring.application.internal.commandservices.RegisterLowStockCommandHandler;
import com.tata.familymonitoring.domain.model.commands.RegisterLowStockCommand;
import com.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class LowStockDetectedEventHandler {
  private final RegisterLowStockCommandHandler registerLowStock;

  public LowStockDetectedEventHandler(RegisterLowStockCommandHandler registerLowStock) {
    this.registerLowStock = registerLowStock;
  }

  @EventListener
  public void handle(LowStockDetected event) {
    registerLowStock.handle(new RegisterLowStockCommand(
        event.medicationId(), event.remainingStock(), event.replenishmentThreshold(), event.detectedAt()));
  }
}
