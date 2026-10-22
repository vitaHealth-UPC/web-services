package com.vitahealth.tata.omissionescalation.application.internal.outboundservices;

import com.vitahealth.tata.omissionescalation.domain.model.events.OmissionEvent;

/** Publishes Omission business facts synchronously inside the caller's transaction. */
public interface IDomainEventPublisher {

  /**
   * Dispatches the concrete event to local consumers. Consumer failures propagate to the caller so
   * its transaction can roll back; publication does not perform device delivery.
   *
   * @param event the non-null business fact to publish
   */
  void publish(OmissionEvent event);
}
