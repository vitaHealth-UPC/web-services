package com.vitahealth.tata.familymonitoring.application.result;

import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import java.time.Instant;
import java.time.LocalDate;

/** Immutable source adherence pattern captured for caregiver follow-up. */
public record AdherenceInsightResult(
    String medicationId,
    String medicationName,
    int omissionDays,
    LocalDate firstDay,
    LocalDate lastDay,
    Instant detectedAt) {
  /** Copies original evidence without retaining a mutable domain entity. */
  public static AdherenceInsightResult from(AdherenceInsight source) {
    return new AdherenceInsightResult(
        source.getMedicationId(),
        source.getMedicationName(),
        source.getOmissionDays(),
        source.getFirstDay(),
        source.getLastDay(),
        source.getDetectedAt());
  }
}
