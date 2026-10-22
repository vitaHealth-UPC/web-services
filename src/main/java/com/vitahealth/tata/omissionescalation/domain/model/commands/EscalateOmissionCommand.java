package com.vitahealth.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

/** Requests the next eligible attention level for one case at the supplied time. */
public record EscalateOmissionCommand(Long omissionCaseId, Instant now) {
  /** Requires an existing case identity and explicit decision time before persistence access. */
  public EscalateOmissionCommand {
    if (omissionCaseId == null || omissionCaseId < 1)
      throw new IllegalArgumentException("positive omissionCaseId is required");
    if (now == null) throw new IllegalArgumentException("decision time is required");
  }
}
