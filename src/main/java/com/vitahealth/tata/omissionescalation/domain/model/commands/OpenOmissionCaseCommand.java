package com.vitahealth.tata.omissionescalation.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;

/** Carries the source intake references and medication snapshot needed to open one pending case. */
public record OpenOmissionCaseCommand(
    String intakeId, String olderAdultId, String medicationName, Instant scheduledAt) {
  /** Validates source references without inventing identity, clinical dose, or occurrence time. */
  public OpenOmissionCaseCommand {
    intakeId = requireText(intakeId, "intakeId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    medicationName = requireText(medicationName, "medicationName");
    if (scheduledAt == null) throw new IllegalArgumentException("scheduled time is required");
  }
}
