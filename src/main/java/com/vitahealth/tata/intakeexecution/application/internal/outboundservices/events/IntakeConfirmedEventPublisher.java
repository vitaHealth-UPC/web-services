package com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;

/** Publishes confirmation evidence synchronously within the confirming transaction. */
@FunctionalInterface
public interface IntakeConfirmedEventPublisher {
  /** Allows dependent inventory and omission consumers to participate in the same transaction. */
  void publish(IntakeConfirmed event);
}
