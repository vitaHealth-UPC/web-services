package com.tata.omissionescalation.application.internal.outboundservices;

/** Publishes domain events inside the same process. */
public interface IDomainEventPublisher {

  void publish(Object event);
}
