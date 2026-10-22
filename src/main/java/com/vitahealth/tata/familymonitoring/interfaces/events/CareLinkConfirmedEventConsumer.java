package com.vitahealth.tata.familymonitoring.interfaces.events;

import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.InitializeFamilyMonitorCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.commands.InitializeFamilyMonitorCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Translates confirmed care relationships into local follow-up initialization. */
@Component
public class CareLinkConfirmedEventConsumer {
  private final InitializeFamilyMonitorCommandHandler initialize;

  /** Injects the local creation boundary that shares the source consent transaction. */
  public CareLinkConfirmedEventConsumer(InitializeFamilyMonitorCommandHandler initialize) {
    this.initialize = initialize;
  }

  /** Preserves relationship identity and participant order during synchronous dispatch. */
  @EventListener
  public void consume(CareLinkConfirmed event) {
    initialize.handle(
        new InitializeFamilyMonitorCommand(
            event.careLinkId(), event.olderAdultId(), event.caregiverId()));
  }
}
