package com.tata.accessibilitypreferences.domain.model.aggregates;

import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Accessibility and interaction preferences of one user. The user is referenced by logical id only.
 */
public final class UserPreferences {
    private final String id;
    private final String userId;
    private TextSizeLevel textSize;
    private boolean highContrast;
    private boolean reducedMotion;
    private boolean readingAssistance;
    private boolean voiceConfirmationEnabled;
    private QuietHoursRange quietHours;
    private List<NotificationChannel> notificationChannels;
    private final Instant createdAt;

    private UserPreferences(
            String id,
            String userId,
            TextSizeLevel textSize,
            boolean highContrast,
            boolean reducedMotion,
            boolean readingAssistance,
            boolean voiceConfirmationEnabled,
            QuietHoursRange quietHours,
            List<NotificationChannel> notificationChannels,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        this.userId = userId.trim();
        this.textSize = Objects.requireNonNull(textSize, "text size is required");
        this.highContrast = highContrast;
        this.reducedMotion = reducedMotion;
        this.readingAssistance = readingAssistance;
        this.voiceConfirmationEnabled = voiceConfirmationEnabled;
        this.quietHours = quietHours;
        this.notificationChannels = validatedChannels(notificationChannels);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static UserPreferences rehydrate(
            String id,
            String userId,
            TextSizeLevel textSize,
            boolean highContrast,
            boolean reducedMotion,
            boolean readingAssistance,
            boolean voiceConfirmationEnabled,
            QuietHoursRange quietHours,
            List<NotificationChannel> notificationChannels,
            Instant createdAt
    ) {
        return new UserPreferences(
                id, userId, textSize, highContrast, reducedMotion, readingAssistance,
                voiceConfirmationEnabled, quietHours, notificationChannels, createdAt);
    }

    public void updateTextSize(TextSizeLevel textSize) {
        this.textSize = Objects.requireNonNull(textSize, "text size is required");
    }

    public void setHighContrast(boolean enabled) {
        this.highContrast = enabled;
    }

    public void setReducedMotion(boolean enabled) {
        this.reducedMotion = enabled;
    }

    public void setReadingAssistance(boolean enabled) {
        this.readingAssistance = enabled;
    }

    public void setVoiceConfirmation(boolean enabled) {
        this.voiceConfirmationEnabled = enabled;
    }

    /** Sets the quiet hours; null removes them. */
    public void setQuietHours(QuietHoursRange quietHours) {
        this.quietHours = quietHours;
    }

    /** Replaces the channel list. Each channel type may appear only once. */
    public void updateChannels(List<NotificationChannel> channels) {
        this.notificationChannels = validatedChannels(channels);
    }

    public boolean isChannelEnabled(ChannelType type) {
        return notificationChannels.stream().anyMatch(channel -> channel.type() == type && channel.enabled());
    }

    private static List<NotificationChannel> validatedChannels(List<NotificationChannel> channels) {
        if (channels == null) {
            throw new IllegalArgumentException("channels are required");
        }
        var seen = new HashSet<ChannelType>();
        for (var channel : channels) {
            if (channel == null) {
                throw new IllegalArgumentException("channel must not be null");
            }
            if (!seen.add(channel.type())) {
                throw new IllegalArgumentException("channel " + channel.type() + " appears more than once");
            }
        }
        return List.copyOf(channels);
    }

    public String id() { return id; }
    public String userId() { return userId; }
    public TextSizeLevel textSize() { return textSize; }
    public boolean highContrast() { return highContrast; }
    public boolean reducedMotion() { return reducedMotion; }
    public boolean readingAssistance() { return readingAssistance; }
    public boolean voiceConfirmationEnabled() { return voiceConfirmationEnabled; }
    public QuietHoursRange quietHours() { return quietHours; }
    public List<NotificationChannel> notificationChannels() { return notificationChannels; }
    public Instant createdAt() { return createdAt; }
}
