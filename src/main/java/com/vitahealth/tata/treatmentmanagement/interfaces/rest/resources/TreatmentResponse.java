package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Exposes treatment lifecycle and nullable draft regimen fields. */
@Schema(
    description =
        "Treatment with its current regimen. Regimen fields are null while the treatment is"
            + " incomplete")
public record TreatmentResponse(
    @Schema(example = "3f6c1d0e-8a52-4f0b-9c55-2f1f4d9a7b10") String id,
    String olderAdultId,
    @Schema(example = "Control de presión") String name,
    @Schema(description = "DRAFT, ACTIVE or PAUSED", example = "DRAFT") String status,
    String medicationId,
    @Schema(example = "1 comprimido") String dose,
    @Schema(example = "DAILY") String frequency,
    @Schema(example = "[\"08:00:00\", \"20:00:00\"]") List<LocalTime> scheduledTimes,
    @Schema(example = "Con un vaso de agua") String instructions,
    @Schema(example = "10") Integer reminderLeadMinutes,
    String recurrenceMode,
    String recurrenceZone,
    Instant recurrenceAnchor,
    Integer followUpDelayMinutes) {
  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public TreatmentResponse(
      String id,
      String olderAdultId,
      String name,
      String status,
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
  public TreatmentResponse(
      String id,
      String olderAdultId,
      String name,
      String status,
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
