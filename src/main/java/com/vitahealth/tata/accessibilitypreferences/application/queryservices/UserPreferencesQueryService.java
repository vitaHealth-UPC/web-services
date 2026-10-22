package com.vitahealth.tata.accessibilitypreferences.application.queryservices;

import com.vitahealth.tata.accessibilitypreferences.application.result.NotificationPreferencesResult;

/** Reads the minimal preference outcomes consumed by other application contexts. */
public interface UserPreferencesQueryService {
  /** Returns saved delivery choices or non-persisted product defaults for this user. */
  NotificationPreferencesResult getNotificationPreferences(String userId);

  /** Reads the current voice setting without initializing or changing any preferences. */
  boolean isVoiceConfirmationEnabled(String userId);
}
