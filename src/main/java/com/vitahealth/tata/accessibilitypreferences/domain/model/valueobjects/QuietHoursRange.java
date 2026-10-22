package com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects;

import java.time.LocalTime;

/**
 * Daily interval in which non-critical notifications are held back. The range may cross midnight,
 * for example 22:00 to 07:00.
 */
public record QuietHoursRange(int startHour, int startMinute, int endHour, int endMinute) {

  /** Requires valid minute-based boundaries and rejects a zero-length daily interval. */
  public QuietHoursRange {
    requireInRange(startHour, 23, "startHour");
    requireInRange(startMinute, 59, "startMinute");
    requireInRange(endHour, 23, "endHour");
    requireInRange(endMinute, 59, "endMinute");
    if (startHour == endHour && startMinute == endMinute) {
      throw new IllegalArgumentException("quiet hours must start and end at different times");
    }
  }

  /** Converts explicit minute-precision times without rounding away seconds or fractions. */
  public static QuietHoursRange between(LocalTime start, LocalTime end) {
    if (start == null
        || end == null
        || start.getSecond() != 0
        || start.getNano() != 0
        || end.getSecond() != 0
        || end.getNano() != 0) {
      throw new IllegalArgumentException(
          "quiet hours require complete times with minute precision");
    }
    return new QuietHoursRange(start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
  }

  /** Returns the inclusive daily start at minute precision. */
  public LocalTime start() {
    return LocalTime.of(startHour, startMinute);
  }

  /** Returns the exclusive daily end at minute precision. */
  public LocalTime end() {
    return LocalTime.of(endHour, endMinute);
  }

  /** True when the given time falls inside the range. The end time itself is outside. */
  public boolean contains(int hour, int minute) {
    requireInRange(hour, 23, "hour");
    requireInRange(minute, 59, "minute");
    int moment = hour * 60 + minute;
    int from = startHour * 60 + startMinute;
    int to = endHour * 60 + endMinute;
    if (from < to) {
      return moment >= from && moment < to;
    }
    return moment >= from || moment < to;
  }

  private static void requireInRange(int value, int max, String field) {
    if (value < 0 || value > max) {
      throw new IllegalArgumentException(field + " must be between 0 and " + max);
    }
  }
}
