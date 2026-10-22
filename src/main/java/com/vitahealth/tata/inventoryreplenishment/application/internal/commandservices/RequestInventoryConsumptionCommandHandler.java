package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.ConfirmedIntakeEvidence;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records recoverable decrement intent atomically with the source confirmation transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class RequestInventoryConsumptionCommandHandler {
  private final ConfirmedIntakeEvidence source;
  private final InventoryConsumptionRequestRepository requests;
  private final InventoryRepository inventories;
  private final Clock clock;

  /** Injects source verification, durable requests, successful consumption evidence and time. */
  public RequestInventoryConsumptionCommandHandler(
      ConfirmedIntakeEvidence source,
      InventoryConsumptionRequestRepository requests,
      InventoryRepository inventories,
      Clock clock) {
    this.source = source;
    this.requests = requests;
    this.inventories = inventories;
    this.clock = clock;
  }

  /** Reserves the source first and never recreates intent for an already recorded decrement. */
  public void handle(RequestInventoryConsumptionCommand command) {
    Objects.requireNonNull(command, "command");
    if (!source.lockAndVerify(command))
      throw new IllegalStateException("inventory consumption source confirmation does not match");
    if (inventories.hasConsumed(command.intakeId())) return;
    requests.enqueue(PendingInventoryConsumption.request(command, clock.instant()));
  }
}
