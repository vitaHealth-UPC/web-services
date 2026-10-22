package com.vitahealth.tata.adherenceanalytics.domain.services;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Identifies recurring issue concentrations across distinct local calendar days. */
public final class AdherenceTimeBandPolicy {
  private static final int MINIMUM_ISSUES = 3;
  private static final double MINIMUM_SHARE = 0.5;

  private AdherenceTimeBandPolicy() {}

  /** Requires at least three issue days in a band containing at least half of all issues. */
  public static Optional<TimeBand> dominantBand(List<Instant> issueSchedules, ZoneId zone) {
    Objects.requireNonNull(zone, "calendar zone");
    var schedules = List.copyOf(issueSchedules);
    Map<TimeBand, Integer> counts = new EnumMap<>(TimeBand.class);
    schedules.forEach(
        schedule ->
            counts.merge(TimeBand.atHour(schedule.atZone(zone).getHour()), 1, Integer::sum));
    var dominant = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
    if (dominant == null
        || dominant.getValue() < MINIMUM_ISSUES
        || dominant.getValue() < schedules.size() * MINIMUM_SHARE) return Optional.empty();
    long days =
        schedules.stream()
            .map(schedule -> schedule.atZone(zone))
            .filter(schedule -> TimeBand.atHour(schedule.getHour()) == dominant.getKey())
            .map(schedule -> schedule.toLocalDate())
            .distinct()
            .count();
    return days < MINIMUM_ISSUES ? Optional.empty() : Optional.of(dominant.getKey());
  }
}
