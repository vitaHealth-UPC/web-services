package com.vitahealth.tata.intakeexecution.domain.model.commands;

import java.time.Instant;

/** Selects unresolved evidence due by the cutoff and records the explicit reporting instant. */
public record ReportUnconfirmedIntakesCommand(Instant cutoff, Instant reportedAt) {
  /** Prevents a reporting marker from predating the evidence evaluation cutoff. */
  public ReportUnconfirmedIntakesCommand {
    if (cutoff == null || reportedAt == null || reportedAt.isBefore(cutoff))
      throw new IllegalArgumentException("reporting requires a cutoff no later than reportedAt");
  }
}
