package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Carries a complete caregiver-requested regimen and optional explicit recurrence metadata. */
public record ConfigureTreatmentCommand(
    String caregiverId,
    String treatmentId,
    String medicationId,
    String dose,
    String frequency,
    List<LocalTime> scheduledTimes,
    String instructions,
    int reminderLeadMinutes,
    String recurrenceMode,
    String recurrenceZone,
    Instant recurrenceAnchor,
    Integer followUpDelayMinutes) {
  /** Captures the requested schedule so caller mutations cannot alter this command. */
  public ConfigureTreatmentCommand {
    scheduledTimes = scheduledTimes == null ? null : List.copyOf(scheduledTimes);
  }

  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public ConfigureTreatmentCommand(
      String caregiverId,
      String treatmentId,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      int reminderLeadMinutes) {
    this(
        caregiverId,
        treatmentId,
        medicationId,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        null,
        null,
        null);
  }

  /** Retains existing callers with the system follow-up default. */
  public ConfigureTreatmentCommand(
      String caregiverId,
      String treatmentId,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      int reminderLeadMinutes,
      String recurrenceMode,
      String recurrenceZone,
      Instant recurrenceAnchor) {
    this(
        caregiverId,
        treatmentId,
        medicationId,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        recurrenceMode,
        recurrenceZone,
        recurrenceAnchor,
        null);
  }
}
