package com.vitahealth.tata.inventoryreplenishment.interfaces.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RequestInventoryConsumptionCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Records durable decrement intent in the source confirmation transaction. */
@Component("inventoryIntakeConfirmedEventConsumer")
public class IntakeConfirmedEventConsumer {
  private final RequestInventoryConsumptionCommandHandler requests;

  /** Injects the local transactional enqueue boundary. */
  public IntakeConfirmedEventConsumer(RequestInventoryConsumptionCommandHandler requests) {
    this.requests = requests;
  }

  /** Maps original source evidence synchronously; storage failure rolls back the confirmation. */
  @EventListener
  public void consume(IntakeConfirmed event) {
    requests.handle(
        new RequestInventoryConsumptionCommand(
            event.intakeId(), event.medicationId(), event.olderAdultId(), event.confirmedAt()));
  }
}
