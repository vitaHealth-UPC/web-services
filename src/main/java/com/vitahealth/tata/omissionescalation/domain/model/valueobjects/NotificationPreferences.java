package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** Recipient channel and complete quiet interval interpreted in an explicitly supplied zone. */
public record NotificationPreferences(
    boolean pushEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd, ZoneId zoneId) {
  /** Rejects partial or ambiguous time evidence instead of silently treating it as unrestricted. */
  public NotificationPreferences {
    Objects.requireNonNull(zoneId, "zoneId");
    if ((quietHoursStart == null) != (quietHoursEnd == null))
      throw new IllegalArgumentException("quiet hours require both boundaries");
    if (quietHoursStart != null
        && (quietHoursStart.equals(quietHoursEnd)
            || quietHoursStart.getSecond() != 0
            || quietHoursStart.getNano() != 0
            || quietHoursEnd.getSecond() != 0
            || quietHoursEnd.getNano() != 0))
      throw new IllegalArgumentException(
          "quiet hours require distinct minute-precision boundaries");
  }

  /** Uses inclusive start and exclusive end for both same-day and overnight quiet intervals. */
  public boolean isQuietAt(Instant moment) {
    Objects.requireNonNull(moment, "moment");
    if (quietHoursStart == null) return false;
    LocalTime time = LocalTime.ofInstant(moment, zoneId);
    if (quietHoursStart.isBefore(quietHoursEnd))
      return !time.isBefore(quietHoursStart) && time.isBefore(quietHoursEnd);
    return !time.isBefore(quietHoursStart) || time.isBefore(quietHoursEnd);
  }
}
