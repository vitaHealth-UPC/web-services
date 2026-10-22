package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Minimal scheduled evidence used for distinct omission-day detection. */
public record AdherenceOmissionObservation(
    String medicationId, Instant scheduledAt, boolean omitted) {
  /** Requires actual source reference and schedule even for a non-omitted observation. */
  public AdherenceOmissionObservation {
    requireText(medicationId, "medicationId");
    Objects.requireNonNull(scheduledAt, "scheduledAt");
  }
}
