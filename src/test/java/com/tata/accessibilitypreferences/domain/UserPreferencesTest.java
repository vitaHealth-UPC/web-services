package com.tata.accessibilitypreferences.domain;

import com.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferencesTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    @Test
    void defaultsAreMediumTextPushOnlyAndNoQuietHours() {
        var preferences = UserPreferencesFactory.createDefaults("user-1", NOW);

        assertEquals(TextSizeLevel.MEDIUM, preferences.textSize());
        assertFalse(preferences.highContrast());
        assertFalse(preferences.reducedMotion());
        assertFalse(preferences.readingAssistance());
        assertTrue(preferences.voiceConfirmationEnabled());
        assertNull(preferences.quietHours());
        assertTrue(preferences.isChannelEnabled(ChannelType.PUSH));
        assertFalse(preferences.isChannelEnabled(ChannelType.SMS));
        assertFalse(preferences.isChannelEnabled(ChannelType.EMAIL));
    }

    @Test
    void userIdIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> UserPreferencesFactory.createDefaults(" ", NOW));
    }

    @Test
    void togglesAndTextSizeAreKept() {
        var preferences = UserPreferencesFactory.createDefaults("user-1", NOW);

        preferences.updateTextSize(TextSizeLevel.EXTRA_LARGE);
        preferences.setHighContrast(true);
        preferences.setReducedMotion(true);
        preferences.setReadingAssistance(true);
        preferences.setVoiceConfirmation(false);

        assertEquals(TextSizeLevel.EXTRA_LARGE, preferences.textSize());
        assertTrue(preferences.highContrast());
        assertTrue(preferences.reducedMotion());
        assertTrue(preferences.readingAssistance());
        assertFalse(preferences.voiceConfirmationEnabled());
    }

    @Test
    void quietHoursInsideTheSameDay() {
        var range = new QuietHoursRange(13, 0, 15, 30);

        assertTrue(range.contains(13, 0));
        assertTrue(range.contains(15, 29));
        assertFalse(range.contains(15, 30));
        assertFalse(range.contains(12, 59));
    }

    @Test
    void quietHoursCrossingMidnight() {
        var range = new QuietHoursRange(22, 0, 7, 0);

        assertTrue(range.contains(23, 30));
        assertTrue(range.contains(0, 0));
        assertTrue(range.contains(6, 59));
        assertFalse(range.contains(7, 0));
        assertFalse(range.contains(12, 0));
        assertTrue(range.contains(22, 0));
    }

    @Test
    void quietHoursRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new QuietHoursRange(24, 0, 7, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuietHoursRange(22, 60, 7, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuietHoursRange(8, 0, 8, 0));
    }

    @Test
    void quietHoursCanBeSetAndRemoved() {
        var preferences = UserPreferencesFactory.createDefaults("user-1", NOW);

        preferences.setQuietHours(new QuietHoursRange(22, 0, 7, 0));
        assertEquals(22, preferences.quietHours().startHour());

        preferences.setQuietHours(null);
        assertNull(preferences.quietHours());
    }

    @Test
    void channelsAreReplacedAsAWhole() {
        var preferences = UserPreferencesFactory.createDefaults("user-1", NOW);

        preferences.updateChannels(List.of(
                new NotificationChannel(ChannelType.PUSH, false),
                new NotificationChannel(ChannelType.EMAIL, true)));

        assertFalse(preferences.isChannelEnabled(ChannelType.PUSH));
        assertTrue(preferences.isChannelEnabled(ChannelType.EMAIL));
        assertEquals(2, preferences.notificationChannels().size());
    }

    @Test
    void aChannelCannotAppearTwice() {
        var preferences = UserPreferencesFactory.createDefaults("user-1", NOW);

        assertThrows(IllegalArgumentException.class, () -> preferences.updateChannels(List.of(
                new NotificationChannel(ChannelType.PUSH, true),
                new NotificationChannel(ChannelType.PUSH, false))));
    }
}
