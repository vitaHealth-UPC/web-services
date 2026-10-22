package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

/** Serializes generation for one treatment even before its first intake row exists. */
@FunctionalInterface
public interface IntakeScheduleLock {
  /** Retains the treatment reservation until the caller transaction commits or rolls back. */
  void reserve(String treatmentId);
}
