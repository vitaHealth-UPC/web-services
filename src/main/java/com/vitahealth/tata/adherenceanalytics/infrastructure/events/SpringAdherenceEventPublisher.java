package com.vitahealth.tata.adherenceanalytics.infrastructure.events;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.AdherenceEventPublisher;
import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Adapts synchronous Spring dispatch without swallowing failures from projection consumers. */
@Component
public class SpringAdherenceEventPublisher implements AdherenceEventPublisher {
  private final ApplicationEventPublisher events;

  /** Injects the runtime event dispatcher. */
  public SpringAdherenceEventPublisher(ApplicationEventPublisher events) {
    this.events = events;
  }

  /** Keeps dispatch inside the source transaction. */
  @Override
  public void publish(AdherencePatternDetected event) {
    events.publishEvent(event);
  }
}
