package com.tata.omissionescalation.infrastructure.modules;

import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import org.springframework.stereotype.Component;

/**
 * Reads notification preferences from Accessibility & Preferences. That module does not expose its
 * public contract yet, so every user gets the default preferences (push enabled, no quiet hours).
 */
@Component
public class NotificationPreferencesAdapter implements INotificationPreferencesPort {

  @Override
  public NotificationPreferences getPreferences(String userId) {
    return NotificationPreferences.defaults();
  }
}
