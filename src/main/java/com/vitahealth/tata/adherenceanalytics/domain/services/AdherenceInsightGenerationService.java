package com.vitahealth.tata.adherenceanalytics.domain.services;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceInsight;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.util.List;

/** Practical follow-up suggestions derived only from observed recurrence. */
public final class AdherenceInsightGenerationService {

  /** Generates follow-up recommendations without changing treatment or medication instructions. */
  public List<AdherenceInsight> generate(List<AdherenceOmissionPattern> patterns) {
    return patterns.stream()
        .map(
            pattern ->
                new AdherenceInsight(
                    pattern.medicationId(),
                    "REVIEW_REMINDER_AND_CAREGIVER_FOLLOW_UP",
                    pattern.omissionDays()))
        .toList();
  }
}
