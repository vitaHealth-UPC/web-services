package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.ConfirmedIntakeEvidence;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomically processes one durable decrement without coupling application logic to source events.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ProcessInventoryConsumptionCommandHandler {
  private final ConfirmedIntakeEvidence source;
  private final InventoryConsumptionRequestRepository requests;
  private final InventoryCommandService inventories;

  /** Injects source reservation, durable intent and the idempotent stock mutation boundary. */
  public ProcessInventoryConsumptionCommandHandler(
      ConfirmedIntakeEvidence source,
      InventoryConsumptionRequestRepository requests,
      InventoryCommandService inventories) {
    this.source = source;
    this.requests = requests;
    this.inventories = inventories;
  }

  /**
   * Reserves source, request and stock in that order. Any failure retains pending intent and rolls
   * back the decrement and marker. A completed request is a no-op on later processing.
   */
  public void handle(RequestInventoryConsumptionCommand command) {
    Objects.requireNonNull(command, "command");
    if (!source.lockAndVerify(command))
      throw new IllegalStateException("inventory consumption source confirmation does not match");
    var request = requests.findByIntakeIdForUpdate(command.intakeId());
    if (request.isEmpty()) return;
    if (!request.get().matches(command))
      throw new IllegalStateException("inventory consumption request evidence does not match");
    inventories.consumeUnit(new ConsumeUnitCommand(command.medicationId(), command.intakeId()));
    requests.complete(command.intakeId());
  }
}
