package com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;

/** Captures domain preference state without exposing the aggregate's mutation capabilities. */
public final class UserPreferencesResultFromAggregateAssembler {
  private UserPreferencesResultFromAggregateAssembler() {}

  /**
   * Copies the completed read or write outcome, retaining stable identity and original timestamps.
   */
  public static UserPreferencesResult toResult(UserPreferences preferences) {
    return new UserPreferencesResult(
        preferences.id(),
        preferences.userId(),
        preferences.textSize(),
        preferences.highContrast(),
        preferences.reducedMotion(),
        preferences.readingAssistance(),
        preferences.voiceConfirmationEnabled(),
        preferences.quietHours(),
        preferences.notificationChannels(),
        preferences.createdAt(),
        preferences.notificationPolicy(),
        preferences.emergencyContacts(),
        preferences.darkTheme());
  }
}
