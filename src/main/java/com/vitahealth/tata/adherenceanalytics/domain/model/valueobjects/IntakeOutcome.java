package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Immutable scheduled outcome evidence used to calculate adherence and omission patterns. */
public record IntakeOutcome(String medicationId, Instant scheduledAt, Status status) {
  /** Outcomes retain pending evidence separately from completed adherence observations. */
  public enum Status {
    PENDING,
    CONFIRMED,
    LATE,
    OMITTED
  }

  /** Requires the original medication reference, scheduled instant and recorded outcome. */
  public IntakeOutcome {
    requireText(medicationId, "medicationId");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
    Objects.requireNonNull(status, "status");
  }
}
