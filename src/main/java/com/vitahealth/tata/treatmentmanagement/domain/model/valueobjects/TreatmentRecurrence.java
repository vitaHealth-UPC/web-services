package com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/** Explicit calendar interpretation and optional first occurrence of a prescribed interval. */
public record TreatmentRecurrence(Mode mode, String zone, Instant anchor) {
  /** Separates local clock schedules from elapsed intervals without parsing clinical free text. */
  public enum Mode {
    LOCAL_TIMES,
    FIXED_INTERVAL
  }

  /** Requires a named zone and an anchor exactly when elapsed recurrence is selected. */
  public TreatmentRecurrence {
    Objects.requireNonNull(mode, "recurrence mode");
    if (zone == null || !ZoneId.getAvailableZoneIds().contains(zone))
      throw new IllegalArgumentException("a named recurrence zone is required");
    zone = ZoneId.of(zone).getId();
    if ((mode == Mode.FIXED_INTERVAL) != (anchor != null))
      throw new IllegalArgumentException("only fixed intervals require an anchor");
    if (anchor != null && anchor.getNano() % 1000 != 0)
      throw new IllegalArgumentException("recurrence anchor must use microsecond precision");
  }

  /** Accepts absent legacy metadata or a complete explicit recurrence, rejecting partial input. */
  public static TreatmentRecurrence fromMetadata(String mode, String zone, Instant anchor) {
    if (mode == null && zone == null && anchor == null) return legacyUtc();
    if (mode == null) throw new IllegalArgumentException("recurrence mode is required");
    return new TreatmentRecurrence(Mode.valueOf(mode), zone, anchor);
  }

  /** Preserves the interpretation of previously stored UTC local-time schedules. */
  public static TreatmentRecurrence legacyUtc() {
    return new TreatmentRecurrence(Mode.LOCAL_TIMES, "UTC", null);
  }

  /** Ensures elapsed recurrence is supported by the prescribed frequency. */
  public void validateFrequency(RegimenFrequency frequency) {
    if (mode == Mode.FIXED_INTERVAL
        && frequency != RegimenFrequency.EVERY_EIGHT_HOURS
        && frequency != RegimenFrequency.EVERY_TWELVE_HOURS)
      throw new IllegalArgumentException(
          "fixed recurrence requires an eight/twelve-hour frequency");
  }
}
