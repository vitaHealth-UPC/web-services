package com.vitahealth.tata.treatmentmanagement.application.events;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Publishes configured treatment evidence for synchronous intake schedule reconciliation. */
public record TreatmentScheduleChangedEvent(
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
  /** Copies schedule values so consumers cannot mutate the published snapshot. */
  public TreatmentScheduleChangedEvent {
    scheduledTimes = scheduledTimes == null ? List.of() : List.copyOf(scheduledTimes);
  }

  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public TreatmentScheduleChangedEvent(
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
