package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Result of a past intake, as shown in the recent history. */
public record IntakeSummary(
    String intakeId, String medicationName, Instant scheduledAt, IntakeStatus status) {
  /** Requires original source evidence while retaining its text, instant and recorded status. */
  public IntakeSummary {
    requireText(intakeId, "intakeId");
    requireText(medicationName, "medicationName");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
    Objects.requireNonNull(status, "status");
  }
}
