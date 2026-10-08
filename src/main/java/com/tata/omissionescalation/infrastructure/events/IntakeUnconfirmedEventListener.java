package com.tata.omissionescalation.infrastructure.events;

import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.omissionescalation.interfaces.events.IntakeUnconfirmedEventConsumer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IntakeUnconfirmedEventListener {

  private final IntakeUnconfirmedEventConsumer consumer;

  public IntakeUnconfirmedEventListener(IntakeUnconfirmedEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventListener
  public void on(IntakeUnconfirmed event) {
    consumer.consume(event);
  }
}
