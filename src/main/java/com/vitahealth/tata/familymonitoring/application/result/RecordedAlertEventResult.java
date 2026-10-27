package com.vitahealth.tata.familymonitoring.application.result;

import java.time.Instant;

/** A recorded occurrence, never an estimate of notification delivery. */
public record RecordedAlertEventResult(Type type, Instant occurredAt) {
  public enum Type {
    INTAKE_SCHEDULED,
    ALERT_GENERATED,
    ALERT_CLOSED
  }
}
