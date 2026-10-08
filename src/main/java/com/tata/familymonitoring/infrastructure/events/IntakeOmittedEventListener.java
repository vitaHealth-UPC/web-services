package com.tata.familymonitoring.infrastructure.events;

import com.tata.familymonitoring.interfaces.events.IntakeOmittedEventConsumer;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IntakeOmittedEventListener {

  private final IntakeOmittedEventConsumer consumer;

  public IntakeOmittedEventListener(IntakeOmittedEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventListener
  public void on(IntakeOmitted event) {
    consumer.consume(event);
  }
}
