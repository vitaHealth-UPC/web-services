package com.vitahealth.tata.omissionescalation.interfaces.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.TrackUnconfirmedIntakeCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Translates an unconfirmed source occurrence into the local tracking command. */
@Component
public class IntakeUnconfirmedEventConsumer {

  private final TrackUnconfirmedIntakeCommandHandler eventHandler;

  /** Injects the transactional local orchestration that reserves the original intake. */
  public IntakeUnconfirmedEventConsumer(TrackUnconfirmedIntakeCommandHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * Preserves source identifiers, medication snapshot and scheduled time without calling a
   * provider.
   */
  @EventListener
  public void consume(IntakeUnconfirmed event) {
    eventHandler.handle(
        new OpenOmissionCaseCommand(
            event.intakeId(), event.olderAdultId(), event.medicationName(), event.scheduledAt()));
  }
}
