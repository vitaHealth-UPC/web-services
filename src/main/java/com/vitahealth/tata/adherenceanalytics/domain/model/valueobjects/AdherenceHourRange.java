package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import java.util.List;
import java.util.Objects;

/** Local clock-hour interval; an end at or before the start belongs to the following day. */
public record AdherenceHourRange(int fromHour, int toHour) {
  /** Requires a meaningful clock interval while retaining 24 as explicit midnight. */
  public AdherenceHourRange {
    if (fromHour < 0 || fromHour > 23 || toHour < 0 || toHour > 24 || fromHour == toHour) {
      throw new IllegalArgumentException("invalid adherence hour interval");
    }
  }

  /** Builds the smallest band-aligned interval covering observed hours, including midnight. */
  public static AdherenceHourRange ofHours(List<Integer> observedHours, TimeBand band) {
    Objects.requireNonNull(band, "time band");
    var hours = List.copyOf(observedHours);
    if (hours.isEmpty()) throw new IllegalArgumentException("observed hours are required");
    int first = Integer.MAX_VALUE;
    int last = Integer.MIN_VALUE;
    for (int hour : hours) {
      if (TimeBand.atHour(hour) != band)
        throw new IllegalArgumentException("hour is outside the detected band");
      int orderedHour = band == TimeBand.NIGHT && hour < 5 ? hour + 24 : hour;
      first = Math.min(first, orderedHour);
      last = Math.max(last, orderedHour);
    }
    int end = last + 1;
    return new AdherenceHourRange(first % 24, end > 24 ? end % 24 : end);
  }
}
