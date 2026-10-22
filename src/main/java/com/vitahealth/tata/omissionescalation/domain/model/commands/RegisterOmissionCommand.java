package com.vitahealth.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

/** Requests the definitive omission decision after the source intake is reserved. */
public record RegisterOmissionCommand(Long omissionCaseId, Instant now) {
  /** Requires an existing case identity and explicit decision time before persistence access. */
  public RegisterOmissionCommand {
    if (omissionCaseId == null || omissionCaseId < 1)
      throw new IllegalArgumentException("positive omissionCaseId is required");
    if (now == null) throw new IllegalArgumentException("decision time is required");
  }
}
