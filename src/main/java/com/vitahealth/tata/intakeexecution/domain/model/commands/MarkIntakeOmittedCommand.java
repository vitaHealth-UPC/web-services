package com.vitahealth.tata.intakeexecution.domain.model.commands;

/** Selects source evidence for a definitive omission transition. */
public record MarkIntakeOmittedCommand(String intakeId) {
  /** Requires an explicit source identifier and normalizes surrounding whitespace. */
  public MarkIntakeOmittedCommand {
    if (intakeId == null || intakeId.isBlank()) {
      throw new IllegalArgumentException("intakeId is required");
    }
    intakeId = intakeId.trim();
  }
}
