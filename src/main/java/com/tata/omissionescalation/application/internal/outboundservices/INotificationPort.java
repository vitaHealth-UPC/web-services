package com.tata.omissionescalation.application.internal.outboundservices;

import java.time.Instant;

/** Sends reminders and caregiver alerts without coupling the application to a push provider. */
public interface INotificationPort {

  NotificationResult sendReinforcedReminder(
      Long olderAdultId, String medicationName, Instant scheduledAt);

  NotificationResult sendCaregiverAlert(
      Long olderAdultId, String medicationName, Instant scheduledAt);

  record NotificationResult(boolean delivered, String failureReason) {

    public static NotificationResult success() {
      return new NotificationResult(true, null);
    }

    public static NotificationResult failure(String reason) {
      return new NotificationResult(false, reason);
    }
  }
}
