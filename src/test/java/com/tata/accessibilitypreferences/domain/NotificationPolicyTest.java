package com.tata.accessibilitypreferences.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPolicyTest {
  @Test
  void timingMustIncreaseFromTheScheduledIntake() {
    assertThrows(
        IllegalArgumentException.class, () -> new NotificationPolicy(0, 20, 120, 180, null));
    assertThrows(
        IllegalArgumentException.class, () -> new NotificationPolicy(10, 10, 120, 180, null));
    assertThrows(
        IllegalArgumentException.class, () -> new NotificationPolicy(10, 20, 120, 100, null));
    assertThrows(
        IllegalArgumentException.class, () -> new NotificationPolicy(10, 20, 120, 1441, null));
    assertNull(new NotificationPolicy(10, 20, 120, 180, null).dailySummaryTime());
  }

  @Test
  void summaryTimeHasMinutePrecision() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new NotificationPolicy(10, 20, 120, 180, LocalTime.of(20, 0, 1)));
  }

  @Test
  void invalidRecipientsDoNotReplaceTheExistingPolicy() {
    var preferences = UserPreferencesFactory.createDefaults("user", Instant.EPOCH);
    var previous = preferences.notificationPolicy();
    var changed = new NotificationPolicy(5, 15, 60, 90, null);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            preferences.updateNotificationPolicy(
                changed,
                List.of(
                    new EmergencyContact("Rosa", "+51 999-123-456"),
                    new EmergencyContact("Diego", "51999123456"))));
    assertEquals(previous, preferences.notificationPolicy());
    assertTrue(preferences.emergencyContacts().isEmpty());
  }

  @Test
  void recipientsAreNormalizedAndTheListCannotBeMutatedExternally() {
    var preferences = UserPreferencesFactory.createDefaults("user", Instant.EPOCH);
    var contacts = new ArrayList<>(List.of(new EmergencyContact(" Rosa ", "+51 (999) 123-456")));
    preferences.updateNotificationPolicy(NotificationPolicy.defaults(), contacts);
    contacts.clear();
    assertEquals(
        new EmergencyContact("Rosa", "+51999123456"), preferences.emergencyContacts().getFirst());
    assertThrows(
        UnsupportedOperationException.class, () -> preferences.emergencyContacts().clear());
    assertThrows(IllegalArgumentException.class, () -> new EmergencyContact("Rosa", "invalid"));
  }
}
