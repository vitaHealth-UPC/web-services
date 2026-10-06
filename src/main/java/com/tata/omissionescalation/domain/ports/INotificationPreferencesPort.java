package com.tata.omissionescalation.domain.ports;

import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;

/** Business-facing port to Accessibility & Preferences. */
public interface INotificationPreferencesPort {

  NotificationPreferences getPreferences(Long userId);
}
