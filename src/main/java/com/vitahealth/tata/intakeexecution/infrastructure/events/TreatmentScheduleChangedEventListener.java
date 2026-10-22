package com.vitahealth.tata.intakeexecution.infrastructure.events;

import com.vitahealth.tata.intakeexecution.interfaces.events.TreatmentScheduleChangedEventConsumer;
import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Binds Spring synchronous delivery to the Intake TreatmentScheduleChanged consumer. */
@Component
public class TreatmentScheduleChangedEventListener {
  private final TreatmentScheduleChangedEventConsumer consumer;

  /** Injects the context-owned inbound event boundary. */
  public TreatmentScheduleChangedEventListener(TreatmentScheduleChangedEventConsumer consumer) {
    this.consumer = consumer;
  }

  /** Preserves the publishing thread and transaction without deferred delivery. */
  @EventListener
  public void on(TreatmentScheduleChangedEvent event) {
    consumer.consume(event);
  }
}
