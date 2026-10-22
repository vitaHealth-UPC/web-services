package com.vitahealth.tata.familymonitoring.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Captures an original repeated-omission pattern for caregiver follow-up projection. */
public record RegisterAdherenceInsightCommand(
    String olderAdultId,
    String medicationId,
    int omissionDays,
    LocalDate firstDay,
    LocalDate lastDay,
    Instant detectedAt) {
  /** Requires complete references, a valid source period and a feasible omission-day count. */
  public RegisterAdherenceInsightCommand {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    medicationId = requireText(medicationId, "medicationId");
    if (firstDay == null || lastDay == null || lastDay.isBefore(firstDay) || detectedAt == null)
      throw new IllegalArgumentException("adherence pattern period and occurrence are required");
    if (omissionDays < 1 || omissionDays > ChronoUnit.DAYS.between(firstDay, lastDay) + 1)
      throw new IllegalArgumentException("omission days exceed the source period");
  }
}
