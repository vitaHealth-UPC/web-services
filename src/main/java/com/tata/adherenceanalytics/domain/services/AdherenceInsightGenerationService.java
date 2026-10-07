package com.tata.adherenceanalytics.domain.services;

import java.util.List;

/** Practical follow-up suggestions derived only from observed recurrence. */
public final class AdherenceInsightGenerationService {
    public record Insight(String medicationId, String code, int evidenceDays) {}
    public List<Insight> generate(List<AdherencePatternDetectionService.Pattern> patterns) {
        return patterns.stream().map(pattern -> new Insight(pattern.medicationId(),
                "REVIEW_REMINDER_AND_CAREGIVER_FOLLOW_UP", pattern.omissionDays())).toList();
    }
}
