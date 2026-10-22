package com.vitahealth.tata.accessibilitypreferences.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Ensures combined decisions own immutable input and validate before changing either preference.
 */
class NotificationPreferencesReplacementTest {
  @Test
  void commandOwnsItsChannelSnapshotAndOneNormalizedUser() {
    var channels = new ArrayList<>(List.of(new NotificationChannel(ChannelType.PUSH, true)));
    var command = new UpdateNotificationPreferencesCommand(" user ", null, channels);
    channels.clear();
    assertEquals("user", command.userId());
    assertEquals(1, command.channels().size());
    assertThrows(UnsupportedOperationException.class, () -> command.channels().clear());
    assertThrows(
        IllegalArgumentException.class,
        () -> new UpdateNotificationPreferencesCommand(" ", null, List.of()));
  }

  @Test
  void invalidChannelsCannotChangeTheExistingQuietInterval() {
    var preferences = UserPreferencesFactory.createDefaults("user", Instant.EPOCH);
    var quiet = new QuietHoursRange(22, 0, 7, 0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            preferences.updateNotificationPreferences(
                quiet,
                List.of(
                    new NotificationChannel(ChannelType.PUSH, false),
                    new NotificationChannel(ChannelType.PUSH, true))));
    assertNull(preferences.quietHours());
    assertTrue(preferences.isChannelEnabled(ChannelType.PUSH));
    assertFalse(preferences.isChannelEnabled(ChannelType.EMAIL));
  }
}
