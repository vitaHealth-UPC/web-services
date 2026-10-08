package com.tata.omissionescalation.infrastructure.events;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.omissionescalation.interfaces.events.IntakeConfirmedEventConsumer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IntakeConfirmedEventListener {

  private final IntakeConfirmedEventConsumer consumer;

  public IntakeConfirmedEventListener(IntakeConfirmedEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventListener
  public void on(IntakeConfirmed event) {
    consumer.consume(event);
  }
}
