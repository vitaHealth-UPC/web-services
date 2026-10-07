package com.tata.familymonitoring.interfaces.events;

import com.tata.familymonitoring.application.internal.eventhandlers.IntakeOmittedEventHandler;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.stereotype.Component;

@Component
public class IntakeOmittedEventConsumer {

  private final IntakeOmittedEventHandler eventHandler;

  public IntakeOmittedEventConsumer(IntakeOmittedEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  public void consume(IntakeOmitted event) {
    eventHandler.handle(event);
  }
}
