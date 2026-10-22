package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Expected treatment slot and medication snapshot, independent of persistence identity. */
public record ScheduledIntakeSlot(
    String treatmentId,
    String olderAdultId,
    String medicationId,
    MedicationSnapshot medication,
    Instant scheduledAt) {
  /** Validates references and immutable evidence before reconciliation begins. */
  public ScheduledIntakeSlot {
    treatmentId = requireText(treatmentId, "treatmentId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    medicationId = requireText(medicationId, "medicationId");
    Objects.requireNonNull(medication, "medication");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
  }
}
