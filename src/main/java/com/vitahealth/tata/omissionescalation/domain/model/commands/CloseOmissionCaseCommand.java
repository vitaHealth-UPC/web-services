package com.vitahealth.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

/** Requests closure of one case at an explicit occurrence time. */
public record CloseOmissionCaseCommand(Long omissionCaseId, Instant now) {
  /** Requires an existing case identity and explicit decision time before persistence access. */
  public CloseOmissionCaseCommand {
    if (omissionCaseId == null || omissionCaseId < 1)
      throw new IllegalArgumentException("positive omissionCaseId is required");
    if (now == null) throw new IllegalArgumentException("decision time is required");
  }
}
