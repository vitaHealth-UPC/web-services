package com.tata.omissionescalation.application.internal.outboundservices.acl;

import com.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import org.springframework.stereotype.Component;

/** Reads channel and quiet-hours preferences through the public contract of Accessibility & Preferences. */
@Component
public class NotificationPreferencesAdapter implements INotificationPreferencesPort {
  private final PreferencesContextFacade preferences;

  public NotificationPreferencesAdapter(PreferencesContextFacade preferences) {
    this.preferences = preferences;
  }

  @Override
  public NotificationPreferences getPreferences(String userId) {
    var result = preferences.getNotificationPreferences(userId);
    return new NotificationPreferences(
        result.pushEnabled(), result.quietHoursStart(), result.quietHoursEnd());
  }
}
