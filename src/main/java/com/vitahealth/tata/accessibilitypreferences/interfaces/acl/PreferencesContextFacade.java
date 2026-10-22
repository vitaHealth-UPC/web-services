package com.vitahealth.tata.accessibilitypreferences.interfaces.acl;

import com.vitahealth.tata.accessibilitypreferences.application.result.NotificationPreferencesResult;

/** Cross-context read contract owned by Accessibility & Preferences, without write capabilities. */
public interface PreferencesContextFacade {
  /** Returns delivery choices with non-persisted defaults when no preferences have been saved. */
  NotificationPreferencesResult getNotificationPreferences(String userId);

  /** Returns the user's current voice choice; a missing record uses the product default. */
  boolean isVoiceConfirmationEnabled(String userId);
}
