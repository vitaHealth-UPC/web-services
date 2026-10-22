package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/** Identifies one reporting candidate and its stable position in a scheduled evidence scan. */
public record IntakeReportingCandidate(String id, Instant scheduledAt) {
  /** Requires the complete position; identifiers are preserved as stored for cursor comparisons. */
  public IntakeReportingCandidate {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("candidate id is required");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
  }
}
