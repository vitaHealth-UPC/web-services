package com.tata.accessibilitypreferences.application.queryservices;

import com.tata.accessibilitypreferences.application.models.NotificationPreferencesResult;

/**
 * Public contract of Accessibility & Preferences for the other modules (Shared Kernel). It never
 * writes: a user without saved preferences gets the defaults.
 */
public interface UserPreferencesQueryService {
    NotificationPreferencesResult getNotificationPreferences(String userId);

    boolean isVoiceConfirmationEnabled(String userId);
}
