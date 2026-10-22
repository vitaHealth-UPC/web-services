package com.vitahealth.tata.intakeexecution.infrastructure.events;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeUnconfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Bridges unresolved-intake events to synchronous transaction-participating Spring consumers. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SpringIntakeUnconfirmedEventPublisher implements IntakeUnconfirmedEventPublisher {
  private final ApplicationEventPublisher publisher;

  /** Injects event delivery without exposing framework publication to application handlers. */
  public SpringIntakeUnconfirmedEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  /** Publishes while the reporting transaction retains its source-evidence locks. */
  @Override
  public void publish(IntakeUnconfirmed event) {
    publisher.publishEvent(event);
  }
}
