package com.vitahealth.tata.omissionescalation.infrastructure.acl;

import com.vitahealth.tata.accessibilitypreferences.interfaces.acl.PreferencesContextFacade;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.vitahealth.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads channel and quiet-hours preferences through the public contract of Accessibility &
 * Preferences.
 */
@Component
public class NotificationPreferencesAdapter implements INotificationPreferencesPort {
  private final PreferencesContextFacade preferences;

  private final ZoneId zoneId;

  /** Injects the public preference boundary and validates the deployment quiet-hours zone. */
  public NotificationPreferencesAdapter(
      PreferencesContextFacade preferences,
      @Value("${tata.notifications.quiet-hours-zone:America/Lima}") String zoneId) {
    this.zoneId = ZoneId.of(zoneId);
    this.preferences = preferences;
  }

  /**
   * Preserves exact channel and interval evidence while keeping the neighboring model outside this
   * context.
   */
  @Override
  public NotificationPreferences getPreferences(String userId) {
    var result = preferences.getNotificationPreferences(userId);
    return new NotificationPreferences(
        result.pushEnabled(), result.quietHoursStart(), result.quietHoursEnd(), zoneId);
  }
}
