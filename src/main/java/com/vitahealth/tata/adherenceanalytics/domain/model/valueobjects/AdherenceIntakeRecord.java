package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import java.time.Instant;

/** Immutable scheduled intake evidence used for adherence history and time-band analysis. */
public record AdherenceIntakeRecord(
    String medicationName, Instant scheduledAt, Instant confirmedAt, Status status) {
  /** Recorded source outcomes; pending schedules are excluded from definitive adherence metrics. */
  public enum Status {
    PENDING,
    CONFIRMED,
    LATE,
    OMITTED
  }
}
