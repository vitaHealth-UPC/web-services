package com.vitahealth.tata.adherenceanalytics.domain.services;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionObservation;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Detects repeated omissions on distinct calendar days, never recommending dosage changes. */
public final class AdherencePatternDetectionService {

  /** Groups actual omissions by medication and local date using the supplied calendar zone. */
  public List<AdherenceOmissionPattern> detect(
      List<AdherenceOmissionObservation> outcomes, ZoneId zone, int minimumDays) {
    if (minimumDays < 2)
      throw new IllegalArgumentException("recurrence requires at least two days");
    Map<String, TreeSet<LocalDate>> days = new TreeMap<>();
    for (var outcome : outcomes) {
      if (outcome.omitted())
        days.computeIfAbsent(outcome.medicationId(), key -> new TreeSet<>())
            .add(outcome.scheduledAt().atZone(zone).toLocalDate());
    }
    return days.entrySet().stream()
        .filter(entry -> entry.getValue().size() >= minimumDays)
        .map(
            entry ->
                new AdherenceOmissionPattern(
                    entry.getKey(),
                    entry.getValue().size(),
                    entry.getValue().first(),
                    entry.getValue().last()))
        .toList();
  }
}
