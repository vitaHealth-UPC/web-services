package com.vitahealth.tata.omissionescalation.interfaces.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.ResolveOmissionCaseCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.ResolveOmissionCaseCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Translates Intake confirmation evidence at the synchronous context event boundary. */
@Component
public class IntakeConfirmedEventConsumer {

  private final ResolveOmissionCaseCommandHandler eventHandler;

  /** Injects the local resolution command without adding another forwarding listener. */
  public IntakeConfirmedEventConsumer(ResolveOmissionCaseCommandHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * Retains the exact recorded confirmation time; the command rechecks source evidence under lock.
   */
  @EventListener
  public void consume(IntakeConfirmed event) {
    eventHandler.handle(new ResolveOmissionCaseCommand(event.intakeId(), event.confirmedAt()));
  }
}
