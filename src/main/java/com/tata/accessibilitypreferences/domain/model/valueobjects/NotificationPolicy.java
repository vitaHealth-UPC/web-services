package com.tata.accessibilitypreferences.domain.model.valueobjects;

import java.time.LocalTime;

/** Notification delays measured from the scheduled intake, independent of treatment dosage. */
public record NotificationPolicy(
    int reminderMinutes,
    int pendingAlertMinutes,
    int omittedAlertMinutes,
    int escalationMinutes,
    LocalTime dailySummaryTime) {
  public NotificationPolicy {
    if (reminderMinutes < 1
        || escalationMinutes > 1440
        || reminderMinutes >= pendingAlertMinutes
        || pendingAlertMinutes >= omittedAlertMinutes
        || omittedAlertMinutes >= escalationMinutes) {
      throw new IllegalArgumentException(
          "Notification delays must increase and stay within one day");
    }
    if (dailySummaryTime != null
        && (dailySummaryTime.getSecond() != 0 || dailySummaryTime.getNano() != 0)) {
      throw new IllegalArgumentException("Daily summary time must have minute precision");
    }
  }

  /** Product defaults; a null summary time disables that notice. */
  public static NotificationPolicy defaults() {
    return new NotificationPolicy(10, 20, 120, 180, LocalTime.of(20, 0));
  }
}
