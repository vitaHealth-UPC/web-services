package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Captures a complete regimen, local schedule and reminder configuration. */
@Schema(description = "Dose, frequency, schedule, instructions and reminder of the treatment")
public record ConfigureTreatmentRequest(
    @Schema(example = "00000000-0000-0000-0000-000000000001") @NotBlank String caregiverId,
    @Schema(description = "Active medication of the same older adult") @NotBlank
        String medicationId,
    @Schema(example = "1 comprimido") @NotBlank @Size(max = 100) String dose,
    @Schema(example = "DAILY") @NotBlank @Size(max = 100) String frequency,
    @Schema(example = "[\"08:00:00\", \"20:00:00\"]") @NotEmpty
        List<@NotNull LocalTime> scheduledTimes,
    @Schema(example = "Con un vaso de agua") @Size(max = 500) String instructions,
    @Schema(description = "Minutes before each intake when the reminder is sent", example = "10")
        @Min(0)
        @Max(1440)
        int reminderLeadMinutes,
    String recurrenceMode,
    String recurrenceZone,
    Instant recurrenceAnchor,
    @Min(0) @Max(1440) Integer followUpDelayMinutes) {
  /** Preserves the UTC local-time contract for callers without explicit recurrence metadata. */
  public ConfigureTreatmentRequest(
      String caregiverId,
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      int reminderLeadMinutes) {
    this(
        caregiverId,
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
  public ConfigureTreatmentRequest(
      String caregiverId,
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
