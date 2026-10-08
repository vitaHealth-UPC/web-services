package com.tata.omissionescalation.infrastructure.events;

import com.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes IntakeOmitted, CaregiverAlertGenerated and EscalationExecuted as in-memory events. */
@Component
public class OmissionDomainEventPublisher implements IDomainEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public OmissionDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(Object event) {
    applicationEventPublisher.publishEvent(event);
  }
}
