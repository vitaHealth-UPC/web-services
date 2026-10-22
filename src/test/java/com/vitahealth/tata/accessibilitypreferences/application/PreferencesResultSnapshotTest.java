package com.vitahealth.tata.accessibilitypreferences.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.UserPreferencesResultFromAggregateAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Proves application outcomes remain stable after aggregate or caller-owned collection changes. */
class PreferencesResultSnapshotTest {
  @Test
  void aggregateMutationCannotChangeAnEarlierApplicationOutcome() {
    var aggregate = UserPreferencesFactory.createDefaults("adult", Instant.EPOCH);
    var snapshot = UserPreferencesResultFromAggregateAssembler.toResult(aggregate);
    aggregate.setVoiceConfirmation(false);
    aggregate.setHighContrast(true);
    aggregate.updateChannels(List.of());
    assertTrue(snapshot.voiceConfirmationEnabled());
    assertFalse(snapshot.highContrast());
    assertTrue(snapshot.isChannelEnabled(ChannelType.PUSH));
    assertEquals(Instant.EPOCH, snapshot.createdAt());
    assertEquals(aggregate.id(), snapshot.id());
  }

  @Test
  void publicResultConstructorOwnsBothListsAndRejectsCollectionMutation() {
    var defaults = UserPreferencesFactory.createDefaults("adult", Instant.EPOCH);
    var channels = new ArrayList<>(defaults.notificationChannels());
    var contacts = new ArrayList<>(List.of(new EmergencyContact("Rosa", "+51999123456")));
    var snapshot =
        new UserPreferencesResult(
            defaults.id(),
            defaults.userId(),
            defaults.textSize(),
            false,
            false,
            false,
            true,
            null,
            channels,
            Instant.EPOCH,
            defaults.notificationPolicy(),
            contacts);
    channels.clear();
    contacts.clear();
    assertEquals(1, snapshot.emergencyContacts().size());
    assertTrue(snapshot.isChannelEnabled(ChannelType.PUSH));
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            snapshot.notificationChannels().add(new NotificationChannel(ChannelType.EMAIL, true)));
    assertThrows(UnsupportedOperationException.class, () -> snapshot.emergencyContacts().clear());
  }
}
