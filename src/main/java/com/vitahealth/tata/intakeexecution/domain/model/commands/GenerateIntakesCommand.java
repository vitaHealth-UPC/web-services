package com.vitahealth.tata.intakeexecution.domain.model.commands;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Requests reconciliation from a complete treatment schedule snapshot. */
public record GenerateIntakesCommand(
    String treatmentId,
    String medicationId,
    String olderAdultId,
    String medicationName,
    String dose,
    String frequency,
    List<LocalTime> scheduledTimes,
    String instructions,
    Integer reminderLeadMinutes,
    boolean active,
    String recurrenceMode,
    String recurrenceZone,
    Instant recurrenceAnchor) {
  /** Snapshots explicit local times; an inactive schedule may carry no times. */
  public GenerateIntakesCommand {
    scheduledTimes = scheduledTimes == null ? List.of() : List.copyOf(scheduledTimes);
  }

  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public GenerateIntakesCommand(
      String treatmentId,
      String medicationId,
      String olderAdultId,
      String medicationName,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      Integer reminderLeadMinutes,
      boolean active) {
    this(
        treatmentId,
        medicationId,
        olderAdultId,
        medicationName,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        active,
        null,
        null,
        null);
  }
}
