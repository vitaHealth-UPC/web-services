package com.vitahealth.tata.intakeexecution.domain.model.commands;

import java.time.Instant;

/** Evaluates one selected intake at the batch's explicit cutoff and reporting instant. */
public record ReportUnconfirmedIntakeCommand(String intakeId, Instant cutoff, Instant reportedAt) {
  /** Requires an evidence reference and a reporting instant no earlier than the cutoff. */
  public ReportUnconfirmedIntakeCommand {
    if (intakeId == null || intakeId.isBlank())
      throw new IllegalArgumentException("intakeId is required");
    if (cutoff == null || reportedAt == null || reportedAt.isBefore(cutoff))
      throw new IllegalArgumentException("reporting requires a cutoff no later than reportedAt");
    intakeId = intakeId.trim();
  }
}
