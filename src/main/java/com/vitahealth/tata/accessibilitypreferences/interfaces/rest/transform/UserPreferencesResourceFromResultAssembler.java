package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.ChannelResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.EmergencyContactResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.NotificationPolicyResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.QuietHoursResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;

/** Converts immutable application outcomes to the existing public preference representation. */
public final class UserPreferencesResourceFromResultAssembler {
  private UserPreferencesResourceFromResultAssembler() {}

  /** Preserves public JSON fields without exposing persistence identity or creation metadata. */
  public static UserPreferencesResource toResource(UserPreferencesResult preferences) {
    var quietHours = preferences.quietHours();
    var policy = preferences.notificationPolicy();
    return new UserPreferencesResource(
        preferences.userId(),
        preferences.textSize(),
        preferences.highContrast(),
        preferences.reducedMotion(),
        preferences.readingAssistance(),
        preferences.voiceConfirmationEnabled(),
        quietHours == null ? null : new QuietHoursResource(quietHours.start(), quietHours.end()),
        preferences.notificationChannels().stream()
            .map(channel -> new ChannelResource(channel.type(), channel.enabled()))
            .toList(),
        new NotificationPolicyResource(
            policy.reminderMinutes(),
            policy.pendingAlertMinutes(),
            policy.omittedAlertMinutes(),
            policy.escalationMinutes(),
            policy.dailySummaryTime()),
        preferences.emergencyContacts().stream()
            .map(c -> new EmergencyContactResource(c.name(), c.phone()))
            .toList(),
        preferences.darkTheme());
  }
}
