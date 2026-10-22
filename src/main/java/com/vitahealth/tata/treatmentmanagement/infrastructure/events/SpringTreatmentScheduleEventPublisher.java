package com.vitahealth.tata.treatmentmanagement.infrastructure.events;

import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Publishes schedule snapshots synchronously inside the owning treatment transaction. */
@Component
public class SpringTreatmentScheduleEventPublisher implements TreatmentScheduleEventPublisher {
  private final ApplicationEventPublisher publisher;

  /** Injects Spring delivery; consumer failures propagate to the publishing transaction. */
  public SpringTreatmentScheduleEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * Requires the current mutation transaction rather than delivering an uncommitted snapshot later.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(TreatmentScheduleChangedEvent event) {
    publisher.publishEvent(Objects.requireNonNull(event, "schedule snapshot"));
  }
}
