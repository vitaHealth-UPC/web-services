package com.vitahealth.tata.familymonitoring.interfaces.events;

import com.vitahealth.tata.familymonitoring.application.internal.commandservices.RegisterAlertFromOmissionCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.commands.RegisterAlertFromOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Translates definitive omission evidence into the local caregiver-alert command. */
@Component
public class IntakeOmittedEventConsumer {

  private final RegisterAlertFromOmissionCommandHandler registerAlert;

  /** Injects the transaction-owning local alert registration boundary. */
  public IntakeOmittedEventConsumer(RegisterAlertFromOmissionCommandHandler registerAlert) {
    this.registerAlert = registerAlert;
  }

  /** Registers the projection synchronously so consumer failures reach the source transaction. */
  @EventListener
  public void consume(IntakeOmitted event) {
    registerAlert.handle(
        new RegisterAlertFromOmissionCommand(
            event.olderAdultId(),
            event.intakeId(),
            event.medicationName(),
            event.scheduledAt(),
            event.reason(),
            event.occurredAt()));
  }
}
