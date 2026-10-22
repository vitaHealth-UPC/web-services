package com.vitahealth.tata.omissionescalation.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;

/** Carries recorded source confirmation evidence for resolving a still-pending omission case. */
public record ResolveOmissionCaseCommand(String intakeId, Instant confirmedAt) {
  /** Requires the exact confirmation time; the handler verifies it against Intake Execution. */
  public ResolveOmissionCaseCommand {
    intakeId = requireText(intakeId, "intakeId");
    if (confirmedAt == null) throw new IllegalArgumentException("confirmation time is required");
  }
}
