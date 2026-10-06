package com.tata.omissionescalation.interfaces.events;

import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.omissionescalation.application.internal.eventhandlers.IntakeUnconfirmedEventHandler;
import org.springframework.stereotype.Component;

@Component
public class IntakeUnconfirmedEventConsumer {

  private final IntakeUnconfirmedEventHandler eventHandler;

  public IntakeUnconfirmedEventConsumer(IntakeUnconfirmedEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  public void consume(IntakeUnconfirmed event) {
    eventHandler.handle(event);
  }
}
