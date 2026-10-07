package com.tata.omissionescalation.interfaces.events;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.omissionescalation.application.internal.eventhandlers.IntakeConfirmedEventHandler;
import org.springframework.stereotype.Component;

@Component
public class IntakeConfirmedEventConsumer {

  private final IntakeConfirmedEventHandler eventHandler;

  public IntakeConfirmedEventConsumer(IntakeConfirmedEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  public void consume(IntakeConfirmed event) {
    eventHandler.handle(event);
  }
}
