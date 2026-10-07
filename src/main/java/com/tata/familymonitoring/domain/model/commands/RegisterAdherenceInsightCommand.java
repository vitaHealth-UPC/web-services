package com.tata.familymonitoring.domain.model.commands;

import java.time.Instant;
import java.time.LocalDate;

public record RegisterAdherenceInsightCommand(
    String olderAdultId,
    String medicationId,
    int omissionDays,
    LocalDate firstDay,
    LocalDate lastDay,
    Instant detectedAt) {
}
