package com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events;

import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;

/** Publishes schedule integration evidence as part of the caller's write transaction. */
public interface TreatmentScheduleEventPublisher {
  /** Delivers synchronously; consumer failures propagate so aggregate changes can roll back. */
  void publish(TreatmentScheduleChangedEvent event);
}
