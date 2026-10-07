package com.tata.omissionescalation.infrastructure.modules;

import com.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import org.springframework.stereotype.Component;

/** Reads channel and quiet-hours preferences through the public contract of Accessibility & Preferences. */
@Component
public class NotificationPreferencesAdapter implements INotificationPreferencesPort {
  private final UserPreferencesQueryService preferences;

  public NotificationPreferencesAdapter(UserPreferencesQueryService preferences) {
    this.preferences = preferences;
  }

  @Override
  public NotificationPreferences getPreferences(String userId) {
    var result = preferences.getNotificationPreferences(userId);
    return new NotificationPreferences(
        result.pushEnabled(), result.quietHoursStart(), result.quietHoursEnd());
  }
}
