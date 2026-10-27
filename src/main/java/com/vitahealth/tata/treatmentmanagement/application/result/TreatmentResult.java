package com.vitahealth.tata.treatmentmanagement.application.result;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Read projection preserving absent regimen fields for an unconfigured draft. */
public record TreatmentResult(
    String id,
    String olderAdultId,
    String name,
    TreatmentStatus status,
    String medicationId,
    String dose,
    String frequency,
    List<LocalTime> scheduledTimes,
    String instructions,
    Integer reminderLeadMinutes,
    String recurrenceMode,
    String recurrenceZone,
    Instant recurrenceAnchor,
    Integer followUpDelayMinutes) {
  /** Snapshots configured times while retaining null for a draft without a regimen. */
  public TreatmentResult {
    scheduledTimes = scheduledTimes == null ? null : List.copyOf(scheduledTimes);
  }

  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public TreatmentResult(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      Integer reminderLeadMinutes) {
    this(
        id,
        olderAdultId,
        name,
        status,
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
  public TreatmentResult(
      String id,
      String olderAdultId,
      String name,
      TreatmentStatus status,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      Integer reminderLeadMinutes,
      String recurrenceMode,
      String recurrenceZone,
      Instant recurrenceAnchor) {
    this(
        id,
        olderAdultId,
        name,
        status,
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
