package com.vitahealth.tata.intakeexecution.infrastructure.events;

import com.vitahealth.tata.intakeexecution.interfaces.events.IntakeOmittedEventConsumer;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Binds Spring synchronous delivery to the Intake IntakeOmitted consumer. */
@Component("intakeExecutionOmittedEventListener")
public class IntakeOmittedEventListener {
  private final IntakeOmittedEventConsumer consumer;

  /** Injects the context-owned inbound event boundary. */
  public IntakeOmittedEventListener(IntakeOmittedEventConsumer consumer) {
    this.consumer = consumer;
  }

  /** Preserves the publishing thread and transaction without deferred delivery. */
  @EventListener
  public void on(IntakeOmitted event) {
    consumer.consume(event);
  }
}
