package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform;

import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateQuietHoursResource;

/** Converts the combined replacement request into one owner-scoped application decision. */
public final class UpdateNotificationPreferencesCommandFromResourceAssembler {
  private UpdateNotificationPreferencesCommandFromResourceAssembler() {}

  /**
   * Constructs validated values before persistence, preserving null as explicit silence removal.
   */
  public static UpdateNotificationPreferencesCommand toCommand(
      String userId, UpdateQuietHoursResource resource) {
    var quiet = resource.quietHours();
    var range = quiet == null ? null : QuietHoursRange.between(quiet.start(), quiet.end());
    var channels =
        resource.channels().stream()
            .map(channel -> new NotificationChannel(channel.type(), channel.enabled()))
            .toList();
    return new UpdateNotificationPreferencesCommand(userId, range, channels);
  }
}
