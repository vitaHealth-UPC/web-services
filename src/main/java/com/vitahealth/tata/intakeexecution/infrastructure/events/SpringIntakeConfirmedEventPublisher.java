package com.vitahealth.tata.intakeexecution.infrastructure.events;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeConfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Bridges the application event port to synchronous Spring event delivery. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SpringIntakeConfirmedEventPublisher implements IntakeConfirmedEventPublisher {
  private final ApplicationEventPublisher publisher;

  /** Injects the framework publisher without exposing it to the command handler. */
  public SpringIntakeConfirmedEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  /** Publishes while the caller retains its transaction and source-evidence row lock. */
  @Override
  public void publish(IntakeConfirmed event) {
    publisher.publishEvent(event);
  }
}
