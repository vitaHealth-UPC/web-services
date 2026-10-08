package com.tata.adherenceanalytics.domain.model.events;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Published by Adherence Analytics when a consolidation finds repeated omissions of a medication.
 * Family Monitoring shows it to the caregiver as an insight.
 */
public record AdherencePatternDetected(
        String olderAdultId,
        String medicationId,
        int omissionDays,
        LocalDate firstDay,
        LocalDate lastDay,
        Instant detectedAt
) {}
