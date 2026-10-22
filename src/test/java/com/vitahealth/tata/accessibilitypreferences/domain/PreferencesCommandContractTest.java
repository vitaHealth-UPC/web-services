package com.vitahealth.tata.accessibilitypreferences.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationChannelsCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateReducedMotionCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Ensures inbound decisions cannot carry absent owners, mutable lists or missing required values.
 */
class PreferencesCommandContractTest {
  private static final NotificationPolicy POLICY = NotificationPolicy.defaults();

  @Test
  void everyCommandAndReadRequiresAndNormalizesTheSameLogicalOwner() {
    List<Function<String, String>> owners =
        List.of(
            user -> new InitializeDefaultPreferencesCommand(user).userId(),
            user -> new UpdateContrastCommand(user, false).userId(),
            user -> new UpdateReducedMotionCommand(user, false).userId(),
            user -> new UpdateReadingAssistanceCommand(user, false).userId(),
            user -> new UpdateVoiceConfirmationCommand(user, false).userId(),
            user -> new UpdateTextSizeCommand(user, TextSizeLevel.MEDIUM).userId(),
            user -> new UpdateQuietHoursCommand(user, null).userId(),
            user -> new UpdateNotificationChannelsCommand(user, List.of()).userId(),
            user -> new UpdateNotificationPolicyCommand(user, POLICY, List.of()).userId(),
            user -> new UpdateNotificationPreferencesCommand(user, null, List.of()).userId(),
            user -> new GetUserPreferencesQuery(user).userId());
    for (var owner : owners) {
      assertThrows(IllegalArgumentException.class, () -> owner.apply(null));
      assertThrows(IllegalArgumentException.class, () -> owner.apply(" \t"));
      assertEquals("adult", owner.apply("\u2003adult\u2003"));
    }
  }

  @Test
  void channelAndContactListsAreIsolatedFromTheirCallers() {
    var channels = new ArrayList<>(List.of(new NotificationChannel(ChannelType.PUSH, true)));
    var contacts = new ArrayList<>(List.of(new EmergencyContact("Rosa", "+51999123456")));
    var channelCommand = new UpdateNotificationChannelsCommand("adult", channels);
    var combined = new UpdateNotificationPreferencesCommand("adult", null, channels);
    var policyCommand = new UpdateNotificationPolicyCommand("adult", POLICY, contacts);
    channels.clear();
    contacts.clear();
    assertEquals(1, channelCommand.channels().size());
    assertEquals(1, combined.channels().size());
    assertEquals(1, policyCommand.contacts().size());
    assertThrows(UnsupportedOperationException.class, () -> channelCommand.channels().clear());
    assertThrows(UnsupportedOperationException.class, () -> policyCommand.contacts().clear());
  }

  @Test
  void missingChoicesFailWhileNullQuietHoursRemainsAnExplicitClear() {
    assertThrows(IllegalArgumentException.class, () -> new UpdateTextSizeCommand("adult", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new UpdateNotificationPolicyCommand("adult", null, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new UpdateNotificationPolicyCommand("adult", POLICY, null));
    assertThrows(
        IllegalArgumentException.class, () -> new UpdateNotificationChannelsCommand("adult", null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new UpdateNotificationChannelsCommand(
                "adult", Arrays.asList((NotificationChannel) null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new UpdateNotificationPreferencesCommand(
                "adult", null, Arrays.asList((NotificationChannel) null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new UpdateNotificationPolicyCommand(
                "adult", POLICY, Arrays.asList((EmergencyContact) null)));
    assertNull(new UpdateQuietHoursCommand("adult", null).quietHours());
    assertNull(new UpdateNotificationPreferencesCommand("adult", null, List.of()).quietHours());
  }
}
