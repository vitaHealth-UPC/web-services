package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Repeated omission evidence for one medication and inclusive calendar span. */
public record AdherenceOmissionPattern(
    String medicationId, int omissionDays, LocalDate firstDay, LocalDate lastDay) {
  /** Requires ordered dates and at least two distinct omission days within their span. */
  public AdherenceOmissionPattern {
    requireText(medicationId, "medicationId");
    Objects.requireNonNull(firstDay, "firstDay");
    Objects.requireNonNull(lastDay, "lastDay");
    if (lastDay.isBefore(firstDay)
        || omissionDays < 2
        || omissionDays > ChronoUnit.DAYS.between(firstDay, lastDay) + 1)
      throw new IllegalArgumentException("invalid repeated omission period");
  }
}
