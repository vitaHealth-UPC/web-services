package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.assemblers;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity;
import com.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity.ChannelEmbeddable;

public final class UserPreferencesPersistenceAssembler {
    private UserPreferencesPersistenceAssembler() {}

    public static UserPreferences toDomain(UserPreferencesPersistenceEntity entity) {
        QuietHoursRange quietHours = entity.getQuietHoursStart() == null || entity.getQuietHoursEnd() == null
                ? null
                : QuietHoursRange.between(entity.getQuietHoursStart(), entity.getQuietHoursEnd());
        return UserPreferences.rehydrate(
                entity.getId(), entity.getUserId(), entity.getTextSize(), entity.isHighContrast(),
                entity.isReducedMotion(), entity.isReadingAssistance(), entity.isVoiceConfirmationEnabled(),
                quietHours,
                entity.getChannels().stream().map(c -> new NotificationChannel(c.getType(), c.isEnabled())).toList(),
                entity.getCreatedAt());
    }

    public static UserPreferencesPersistenceEntity toEntity(UserPreferences preferences) {
        var quietHours = preferences.quietHours();
        return new UserPreferencesPersistenceEntity(
                preferences.id(), preferences.userId(), preferences.textSize(), preferences.highContrast(),
                preferences.reducedMotion(), preferences.readingAssistance(), preferences.voiceConfirmationEnabled(),
                quietHours == null ? null : quietHours.start(),
                quietHours == null ? null : quietHours.end(),
                preferences.notificationChannels().stream()
                        .map(c -> new ChannelEmbeddable(c.type(), c.enabled()))
                        .toList(),
                preferences.createdAt());
    }
}
