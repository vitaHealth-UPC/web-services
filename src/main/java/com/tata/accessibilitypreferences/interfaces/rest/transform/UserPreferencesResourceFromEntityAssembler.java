package com.tata.accessibilitypreferences.interfaces.rest.transform;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.interfaces.rest.resources.ChannelResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.QuietHoursResource;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UserPreferencesResource;

public final class UserPreferencesResourceFromEntityAssembler {
    private UserPreferencesResourceFromEntityAssembler() {}

    public static UserPreferencesResource toResourceFromEntity(UserPreferences preferences) {
        var quietHours = preferences.quietHours();
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
                        .toList());
    }
}
