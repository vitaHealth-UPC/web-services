package com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events;

import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;

/** Publishes unresolved evidence in the same transaction that records its handoff marker. */
@FunctionalInterface
public interface IntakeUnconfirmedEventPublisher {
  /** Propagates consumer failures so a failed handoff can be retried without losing evidence. */
  void publish(IntakeUnconfirmed event);
}
