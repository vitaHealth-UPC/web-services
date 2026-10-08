package com.tata.accessibilitypreferences.domain.factories;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserPreferencesFactory {
    private UserPreferencesFactory() {}

    /** Medium text, no visual adjustments, voice confirmation on, push only and no quiet hours. */
    public static UserPreferences createDefaults(String userId, Instant now) {
        return UserPreferences.rehydrate(
                UUID.randomUUID().toString(),
                userId,
                TextSizeLevel.MEDIUM,
                false,
                false,
                false,
                true,
                null,
                List.of(
                        new NotificationChannel(ChannelType.PUSH, true),
                        new NotificationChannel(ChannelType.SMS, false),
                        new NotificationChannel(ChannelType.EMAIL, false)),
                now);
    }
}
