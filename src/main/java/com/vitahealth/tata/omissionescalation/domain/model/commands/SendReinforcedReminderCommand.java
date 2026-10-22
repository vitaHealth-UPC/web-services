package com.vitahealth.tata.omissionescalation.domain.model.commands;

/** Requests a reminder decision for one existing omission case. */
public record SendReinforcedReminderCommand(Long omissionCaseId) {
  /** Requires the case identity before the handler reads its current preferences and state. */
  public SendReinforcedReminderCommand {
    if (omissionCaseId == null || omissionCaseId < 1)
      throw new IllegalArgumentException("positive omissionCaseId is required");
  }
}
