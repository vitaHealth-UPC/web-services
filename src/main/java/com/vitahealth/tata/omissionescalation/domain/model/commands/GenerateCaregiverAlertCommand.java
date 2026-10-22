package com.vitahealth.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

/** Requests creation or retry of the active case alert without duplicating business evidence. */
public record GenerateCaregiverAlertCommand(Long omissionCaseId, Instant now) {
  /** Requires an existing case identity and explicit decision time before persistence access. */
  public GenerateCaregiverAlertCommand {
    if (omissionCaseId == null || omissionCaseId < 1)
      throw new IllegalArgumentException("positive omissionCaseId is required");
    if (now == null) throw new IllegalArgumentException("decision time is required");
  }
}
