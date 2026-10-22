package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Owns the validated recurrence interpretation received from a treatment snapshot. */
public record IntakeRecurrence(
    Mode mode, ScheduleTimeZone zone, Instant anchor, int intervalHours) {
  /** Distinguishes calendar clock repetition from elapsed-hour recurrence. */
  public enum Mode {
    LOCAL_TIMES,
    FIXED_INTERVAL
  }

  /** Rejects partial or inconsistent metadata before generating clinical slots. */
  public IntakeRecurrence {
    Objects.requireNonNull(mode, "recurrence mode");
    Objects.requireNonNull(zone, "recurrence zone");
    if (mode == Mode.LOCAL_TIMES && (anchor != null || intervalHours != 0))
      throw new IllegalArgumentException("local recurrence cannot have an interval anchor");
    if (mode == Mode.FIXED_INTERVAL
        && (anchor == null || (intervalHours != 8 && intervalHours != 12)))
      throw new IllegalArgumentException(
          "fixed recurrence requires an anchor and eight/twelve hours");
    if (anchor != null && anchor.getNano() % 1000 != 0)
      throw new IllegalArgumentException("recurrence anchor must use microsecond precision");
  }

  /** Maps complete integration metadata, preserving the all-absent UTC local-time contract. */
  public static IntakeRecurrence fromSnapshot(
      String mode, String zone, Instant anchor, String frequency) {
    if (mode == null && zone == null && anchor == null)
      return new IntakeRecurrence(Mode.LOCAL_TIMES, new ScheduleTimeZone("UTC"), null, 0);
    if (mode == null) throw new IllegalArgumentException("recurrence mode is required");
    var kind = Mode.valueOf(mode);
    int hours = 0;
    if (kind == Mode.FIXED_INTERVAL) {
      if (frequency == null) throw new IllegalArgumentException("interval frequency is required");
      hours =
          switch (frequency.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")) {
            case "cada 8 horas" -> 8;
            case "cada 12 horas" -> 12;
            default -> throw new IllegalArgumentException("unsupported interval frequency");
          };
    }
    return new IntakeRecurrence(kind, new ScheduleTimeZone(zone), anchor, hours);
  }
}
