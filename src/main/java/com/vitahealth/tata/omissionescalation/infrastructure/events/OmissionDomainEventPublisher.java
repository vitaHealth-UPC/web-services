package com.vitahealth.tata.omissionescalation.infrastructure.events;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.domain.model.events.OmissionEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Bridges typed Omission facts to Spring's synchronous local event dispatch. */
@Component
public class OmissionDomainEventPublisher implements IDomainEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  /** Injects the local publisher without introducing an asynchronous executor or error handler. */
  public OmissionDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(OmissionEvent event) {
    applicationEventPublisher.publishEvent(Objects.requireNonNull(event, "event is required"));
  }
}
