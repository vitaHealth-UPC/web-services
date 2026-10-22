package com.vitahealth.tata.accessibilitypreferences.domain.factories;

import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Creates product preference defaults at the explicit time supplied by the application. */
public final class UserPreferencesFactory {
  private UserPreferencesFactory() {}

  /** Medium text, voice confirmation on, push enabled and the other three channels disabled. */
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
            new NotificationChannel(ChannelType.EMAIL, false),
            new NotificationChannel(ChannelType.CALL, false)),
        now);
  }
}
