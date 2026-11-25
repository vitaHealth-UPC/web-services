package com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Identifies supported frequency choices; generation anchors and time zones belong to recurrence.
 */
public enum RegimenFrequency {
  /** A schedule repeated on each local calendar day. */
  DAILY,
  /** Exactly one explicitly configured local time per day. */
  ONCE_DAILY,
  /**
   * A prescribed eight-hour interval, requiring explicit recurrence configuration for generation.
   */
  EVERY_EIGHT_HOURS,
  /**
   * A prescribed twelve-hour interval, requiring explicit recurrence configuration for generation.
   */
  EVERY_TWELVE_HOURS,
  /** Explicit prescribed times without inferring an interval from free text. */
  PRESCRIBED_TIMES,
  /**
   * Any other whole-hour interval that completes a day ({@link #SUPPORTED_INTERVALS}), written as
   * "Cada N horas"; the hours are read again from the label with {@link #intervalHours(String)}.
   */
  EVERY_INTERVAL;

  /** Whole-hour intervals whose doses repeat at the same local times every day. */
  public static final Set<Integer> SUPPORTED_INTERVALS = Set.of(2, 3, 4, 6, 8, 12);

  private static final Pattern INTERVAL = Pattern.compile("cada (\\d{1,2}) horas");

  /** Accepts proven mobile/API labels without inventing a daily fallback for unknown input. */
  public static RegimenFrequency fromLabel(String label) {
    var normalized = normalize(label);
    return switch (normalized) {
      case "daily", "cada dia", "diaria" -> DAILY;
      case "una vez al dia", "cada 24 horas" -> ONCE_DAILY;
      case "cada 8 horas" -> EVERY_EIGHT_HOURS;
      case "cada 12 horas" -> EVERY_TWELVE_HOURS;
      case "segun indicacion medica" -> PRESCRIBED_TIMES;
      default -> {
        if (intervalHours(label) == null)
          throw new IllegalArgumentException("unsupported treatment frequency");
        yield EVERY_INTERVAL;
      }
    };
  }

  /** Returns the hours of an interval label ("Cada 6 horas"), or null for other frequencies. */
  public static Integer intervalHours(String label) {
    var matcher = INTERVAL.matcher(normalize(label));
    if (!matcher.matches()) return null;
    var hours = Integer.parseInt(matcher.group(1));
    return SUPPORTED_INTERVALS.contains(hours) ? hours : null;
  }

  /** Validates explicit local times without inventing missing doses or deriving them from text. */
  public void validateScheduledTimes(List<LocalTime> times) {
    validateScheduledTimes(
        times, this == EVERY_EIGHT_HOURS ? 8 : this == EVERY_TWELVE_HOURS ? 12 : null);
  }

  /**
   * Validates explicit local times; an interval needs one dose every {@code intervalHours}, so a
   * complete day holds {@code 24 / intervalHours} equidistant times, including across midnight.
   */
  public void validateScheduledTimes(List<LocalTime> times, Integer intervalHours) {
    var sorted = List.copyOf(times).stream().distinct().sorted().toList();
    if (sorted.isEmpty())
      throw new IllegalArgumentException("at least one scheduled time is required");
    if (this == ONCE_DAILY && sorted.size() != 1)
      throw new IllegalArgumentException(
          "once daily frequency requires exactly one scheduled time");
    if (this != EVERY_EIGHT_HOURS && this != EVERY_TWELVE_HOURS && this != EVERY_INTERVAL) return;
    if (intervalHours == null || !SUPPORTED_INTERVALS.contains(intervalHours))
      throw new IllegalArgumentException("unsupported treatment interval");
    if (sorted.size() != 24 / intervalHours)
      throw new IllegalArgumentException("interval frequency requires a complete daily time cycle");
    var expected = Duration.ofHours(intervalHours);
    for (int index = 0; index < sorted.size(); index++) {
      var current = sorted.get(index);
      var next = sorted.get((index + 1) % sorted.size());
      var elapsed = Duration.between(current, next);
      if (elapsed.isNegative() || elapsed.isZero()) elapsed = elapsed.plusDays(1);
      if (!elapsed.equals(expected))
        throw new IllegalArgumentException("scheduled times do not match the prescribed interval");
    }
  }

  private static String normalize(String label) {
    return Normalizer.normalize(requireText(label, "frequency"), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ");
  }
}
