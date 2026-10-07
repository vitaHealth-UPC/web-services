package com.tata.omissionescalation.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/** Channel and quiet-hours preferences of a user, as provided by Accessibility & Preferences. */
public record NotificationPreferences(
    boolean pushEnabled,
    LocalTime quietHoursStart,
    LocalTime quietHoursEnd) {

  private static final ZoneId LOCAL_ZONE = ZoneId.of("America/Lima");

  public static NotificationPreferences defaults() {
    return new NotificationPreferences(true, null, null);
  }

  public boolean isQuietAt(Instant moment) {
    if (quietHoursStart == null || quietHoursEnd == null) {
      return false;
    }
    LocalTime time = LocalTime.ofInstant(moment, LOCAL_ZONE);
    if (quietHoursStart.isBefore(quietHoursEnd)) {
      return !time.isBefore(quietHoursStart) && time.isBefore(quietHoursEnd);
    }
    return !time.isBefore(quietHoursStart) || time.isBefore(quietHoursEnd);
  }
}
