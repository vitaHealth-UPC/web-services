package com.vitahealth.tata.omissionescalation.domain.ports;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;

/** Business-facing port to Accessibility & Preferences. */
public interface INotificationPreferencesPort {

  /**
   * Reads current recipient delivery preferences; technical lookup failures must remain visible.
   */
  NotificationPreferences getPreferences(String userId);
}
