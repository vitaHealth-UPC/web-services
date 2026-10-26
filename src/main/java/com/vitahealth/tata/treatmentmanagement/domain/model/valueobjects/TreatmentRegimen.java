package com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/** Immutable configured dosage and normalized local times within persistence field bounds. */
public record TreatmentRegimen(
    String medicationId,
    String dose,
    String frequency,
    List<LocalTime> scheduledTimes,
    String instructions,
    int reminderLeadMinutes,
    TreatmentRecurrence recurrence,
    Integer followUpDelayMinutes) {
  /** Validates the full value and snapshots sorted distinct scheduled times. */
  public TreatmentRegimen {
    medicationId = requireText(medicationId, "medicationId");
    dose = requireText(dose, "dose");
    frequency = requireText(frequency, "frequency");
    if (dose.length() > 100 || frequency.length() > 100) {
      throw new IllegalArgumentException("dose and frequency must not exceed 100 characters");
    }
    var frequencyKind = RegimenFrequency.fromLabel(frequency);
    if (scheduledTimes == null || scheduledTimes.isEmpty()) {
      throw new IllegalArgumentException("at least one scheduled time is required");
    }
    scheduledTimes =
        List.copyOf(
            scheduledTimes.stream()
                .map(time -> Objects.requireNonNull(time, "scheduled time is required"))
                .distinct()
                .sorted()
                .toList());
    Objects.requireNonNull(recurrence, "recurrence").validateFrequency(frequencyKind);
    frequencyKind.validateScheduledTimes(scheduledTimes);
    instructions = instructions == null ? "" : instructions.trim();
    if (instructions.length() > 500) {
      throw new IllegalArgumentException("instructions must not exceed 500 characters");
    }
    if (followUpDelayMinutes != null && (followUpDelayMinutes < 0 || followUpDelayMinutes > 1440)) {
      throw new IllegalArgumentException("followUpDelayMinutes must be between 0 and 1440");
    }
    if (reminderLeadMinutes < 0 || reminderLeadMinutes > 24 * 60) {
      throw new IllegalArgumentException("reminderLeadMinutes must be between 0 and 1440");
    }
  }

  /** Preserves UTC local-time interpretation for existing callers and stored schedules. */
  public TreatmentRegimen(
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      int reminderLeadMinutes) {
    this(
        medicationId,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        TreatmentRecurrence.legacyUtc());
  }

  /** Retains existing callers with the system follow-up default. */
  public TreatmentRegimen(
      String medicationId,
      String dose,
      String frequency,
      List<LocalTime> scheduledTimes,
      String instructions,
      int reminderLeadMinutes,
      TreatmentRecurrence recurrence) {
    this(
        medicationId,
        dose,
        frequency,
        scheduledTimes,
        instructions,
        reminderLeadMinutes,
        recurrence,
        null);
  }
}
