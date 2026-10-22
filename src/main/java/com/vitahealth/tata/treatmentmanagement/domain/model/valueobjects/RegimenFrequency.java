package com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

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
  PRESCRIBED_TIMES;

  /** Accepts proven mobile/API labels without inventing a daily fallback for unknown input. */
  public static RegimenFrequency fromLabel(String label) {
    var normalized =
        Normalizer.normalize(requireText(label, "frequency"), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
    return switch (normalized) {
      case "daily", "cada dia", "diaria" -> DAILY;
      case "una vez al dia", "cada 24 horas" -> ONCE_DAILY;
      case "cada 8 horas" -> EVERY_EIGHT_HOURS;
      case "cada 12 horas" -> EVERY_TWELVE_HOURS;
      case "segun indicacion medica" -> PRESCRIBED_TIMES;
      default -> throw new IllegalArgumentException("unsupported treatment frequency");
    };
  }

  /** Validates explicit local times without inventing missing doses or deriving them from text. */
  public void validateScheduledTimes(List<LocalTime> times) {
    var sorted = List.copyOf(times).stream().distinct().sorted().toList();
    if (sorted.isEmpty())
      throw new IllegalArgumentException("at least one scheduled time is required");
    if (this == ONCE_DAILY && sorted.size() != 1)
      throw new IllegalArgumentException(
          "once daily frequency requires exactly one scheduled time");
    if (this != EVERY_EIGHT_HOURS && this != EVERY_TWELVE_HOURS) return;
    var hours = this == EVERY_EIGHT_HOURS ? 8 : 12;
    if (sorted.size() != 24 / hours)
      throw new IllegalArgumentException("interval frequency requires a complete daily time cycle");
    var expected = Duration.ofHours(hours);
    for (int index = 0; index < sorted.size(); index++) {
      var current = sorted.get(index);
      var next = sorted.get((index + 1) % sorted.size());
      var elapsed = Duration.between(current, next);
      if (elapsed.isNegative() || elapsed.isZero()) elapsed = elapsed.plusDays(1);
      if (!elapsed.equals(expected))
        throw new IllegalArgumentException("scheduled times do not match the prescribed interval");
    }
  }
}
