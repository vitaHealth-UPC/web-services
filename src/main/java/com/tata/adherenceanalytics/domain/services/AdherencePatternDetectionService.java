package com.tata.adherenceanalytics.domain.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Detects repeated omissions on distinct calendar days, never recommending dosage changes. */
public final class AdherencePatternDetectionService {
    public record Outcome(String medicationId, Instant scheduledAt, boolean omitted) {}
    public record Pattern(String medicationId, int omissionDays, LocalDate firstDay, LocalDate lastDay) {}
    public List<Pattern> detect(List<Outcome> outcomes, ZoneId zone, int minimumDays) {
        if (minimumDays < 2) throw new IllegalArgumentException("recurrence requires at least two days");
        Map<String, TreeSet<LocalDate>> days = new TreeMap<>();
        for (var outcome : outcomes) {
            if (outcome.omitted()) days.computeIfAbsent(outcome.medicationId(), key -> new TreeSet<>())
                    .add(outcome.scheduledAt().atZone(zone).toLocalDate());
        }
        return days.entrySet().stream().filter(entry -> entry.getValue().size() >= minimumDays)
                .map(entry -> new Pattern(entry.getKey(), entry.getValue().size(),
                        entry.getValue().first(), entry.getValue().last())).toList();
    }
}
