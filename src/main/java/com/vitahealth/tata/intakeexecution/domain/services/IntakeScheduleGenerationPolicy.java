package com.vitahealth.tata.intakeexecution.domain.services;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeRecurrence;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ScheduleTimeZone;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Calculates expected instants from explicit local times and a supplied calendar zone. */
public final class IntakeScheduleGenerationPolicy {
  private IntakeScheduleGenerationPolicy() {}

  /** Chooses the validated recurrence rule without application or persistence dependencies. */
  public static List<Instant> occurrences(
      IntakeRecurrence recurrence, List<LocalTime> times, Instant from, Instant to) {
    Objects.requireNonNull(recurrence, "recurrence");
    return switch (recurrence.mode()) {
      case LOCAL_TIMES -> dailyTimes(times, recurrence.zone(), from, to);
      case FIXED_INTERVAL ->
          fixedInterval(recurrence.anchor(), recurrence.intervalHours(), from, to);
    };
  }

  /** Returns midnight after the specified number of local dates, rather than elapsed 24h days. */
  public static Instant horizonEnd(Instant from, ScheduleTimeZone zone, int calendarDays) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(zone, "zone");
    if (calendarDays < 1) throw new IllegalArgumentException("calendar horizon must be positive");
    return LocalDate.ofInstant(from, zone.zoneId())
        .plusDays(calendarDays)
        .atStartOfDay(zone.zoneId())
        .toInstant();
  }

  /**
   * Generates prescribed elapsed eight/twelve-hour intervals from an explicit first occurrence. The
   * anchor is retained across horizon extensions; calendar offset changes do not change the elapsed
   * interval. Results lie in [from,to), never precede the anchor and span at most eight days.
   */
  public static List<Instant> fixedInterval(
      Instant anchor, int intervalHours, Instant from, Instant to) {
    Objects.requireNonNull(anchor, "anchor");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (intervalHours != 8 && intervalHours != 12)
      throw new IllegalArgumentException("supported interval must be eight or twelve hours");
    if (!to.isAfter(from) || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0)
      throw new IllegalArgumentException(
          "interval generation requires a horizon of at most eight days");
    var interval = Duration.ofHours(intervalHours);
    var next = anchor;
    if (from.isAfter(anchor)) {
      long elapsedCycles = Duration.between(anchor, from).dividedBy(interval);
      next = anchor.plus(interval.multipliedBy(elapsedCycles));
      if (next.isBefore(from)) {
        if (Duration.between(next, to).compareTo(interval) <= 0) return List.of();
        next = next.plus(interval);
      }
    }
    var result = new ArrayList<Instant>();
    while (next.isBefore(to)) {
      result.add(next);
      // Avoid overflowing Instant near its maximum when the next slot is outside the horizon.
      if (Duration.between(next, to).compareTo(interval) <= 0) break;
      next = next.plus(interval);
    }
    return List.copyOf(result);
  }

  /**
   * Repeats distinct local times in the half-open interval. A daylight-saving gap shifts a time
   * forward by the gap duration; an overlap uses the earlier offset exactly once. Distinct local
   * times that collapse to one instant are rejected rather than silently losing a prescribed slot.
   */
  public static List<Instant> dailyTimes(
      List<LocalTime> times, ScheduleTimeZone zone, Instant from, Instant to) {
    Objects.requireNonNull(zone, "zone");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (!to.isAfter(from))
      throw new IllegalArgumentException("generation requires an ordered horizon");
    var localTimes = List.copyOf(times).stream().distinct().sorted().toList();
    if (localTimes.isEmpty())
      throw new IllegalArgumentException("at least one scheduled time is required");
    var first = LocalDate.ofInstant(from, zone.zoneId());
    var last = LocalDate.ofInstant(to, zone.zoneId());
    var instants = new TreeSet<Instant>();
    for (var date = first; !date.isAfter(last); date = date.plusDays(1)) {
      for (var time : localTimes) {
        var instant = date.atTime(time).atZone(zone.zoneId()).toInstant();
        if (!instant.isBefore(from) && instant.isBefore(to) && !instants.add(instant)) {
          throw new IllegalArgumentException(
              "distinct local schedule times resolve to one instant");
        }
      }
    }
    return List.copyOf(instants);
  }
}
