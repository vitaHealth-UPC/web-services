package com.vitahealth.tata.familymonitoring.application.result;

import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertSeverity;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import java.time.Instant;

/** Immutable alert evidence returned after a scoped query or persisted transition. */
public record AlertResult(
    Long id,
    String intakeId,
    String medicationName,
    Instant scheduledAt,
    String reason,
    AlertStatus status,
    Instant openedAt,
    Instant closedAt,
    AlertSeverity severity) {
  /** Copies original evidence without retaining a mutable domain entity. */
  public static AlertResult from(AlertSummary source) {
    return from(source, AlertSeverity.UNKNOWN);
  }

  /** Copies source evidence and the separately queried operational priority. */
  public static AlertResult from(AlertSummary source, AlertSeverity severity) {
    return new AlertResult(
        source.getId(),
        source.getIntakeId(),
        source.getMedicationName(),
        source.getScheduledAt(),
        source.getReason(),
        source.getStatus(),
        source.getOpenedAt(),
        source.getClosedAt(),
        severity);
  }
}
