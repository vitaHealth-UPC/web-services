package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.assemblers.UserPreferencesPersistenceAssembler;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.NotificationPolicyEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Distinguishes absent legacy configuration from incomplete persisted evidence without repair. */
class PreferencesStoredStateTest {
  @Test
  void partialQuietHoursAreNotDiscardedOrRounded() {
    var startOnly = entity(LocalTime.of(22, 0), null);
    var endOnly = entity(null, LocalTime.of(7, 0));
    var fractional = entity(LocalTime.of(22, 0, 1), LocalTime.of(7, 0));
    for (var stored : List.of(startOnly, endOnly, fractional)) {
      assertThrows(
          IllegalStateException.class, () -> UserPreferencesPersistenceAssembler.toDomain(stored));
    }
    assertEquals(LocalTime.of(22, 0), startOnly.getQuietHoursStart());
    assertNull(startOnly.getQuietHoursEnd());
  }

  @Test
  void entirelyAbsentLegacyPolicyUsesDefaultsWithoutWritingAnEmbeddable() {
    var absent = entity(null, null);
    assertEquals(
        NotificationPolicy.defaults(),
        UserPreferencesPersistenceAssembler.toDomain(absent).notificationPolicy());
    assertNull(absent.getNotificationPolicy());
    var allNull = mock(NotificationPolicyEmbeddable.class);
    when(allNull.getReminderMinutes()).thenReturn(null);
    when(allNull.getPendingAlertMinutes()).thenReturn(null);
    when(allNull.getOmittedAlertMinutes()).thenReturn(null);
    when(allNull.getEscalationMinutes()).thenReturn(null);
    absent.setNotificationPolicy(allNull);
    assertEquals(
        NotificationPolicy.defaults(),
        UserPreferencesPersistenceAssembler.toDomain(absent).notificationPolicy());
  }

  @Test
  void everyMissingRequiredDelayAndSummaryOnlyPolicyIsRejected() {
    for (int missing = 0; missing < 4; missing++) {
      var policy = mock(NotificationPolicyEmbeddable.class);
      when(policy.getReminderMinutes()).thenReturn(missing == 0 ? null : 10);
      when(policy.getPendingAlertMinutes()).thenReturn(missing == 1 ? null : 20);
      when(policy.getOmittedAlertMinutes()).thenReturn(missing == 2 ? null : 120);
      when(policy.getEscalationMinutes()).thenReturn(missing == 3 ? null : 180);
      var stored = entity(null, null);
      stored.setNotificationPolicy(policy);
      assertThrows(
          IllegalStateException.class, () -> UserPreferencesPersistenceAssembler.toDomain(stored));
    }
    var summaryOnly = mock(NotificationPolicyEmbeddable.class);
    when(summaryOnly.getReminderMinutes()).thenReturn(null);
    when(summaryOnly.getPendingAlertMinutes()).thenReturn(null);
    when(summaryOnly.getOmittedAlertMinutes()).thenReturn(null);
    when(summaryOnly.getEscalationMinutes()).thenReturn(null);
    when(summaryOnly.getDailySummaryTime()).thenReturn(LocalTime.of(20, 0));
    var stored = entity(null, null);
    stored.setNotificationPolicy(summaryOnly);
    assertThrows(
        IllegalStateException.class, () -> UserPreferencesPersistenceAssembler.toDomain(stored));
  }

  @Test
  void completePolicyKeepsDeliberatelyDisabledSummary() {
    var stored = entity(null, null);
    stored.setNotificationPolicy(new NotificationPolicyEmbeddable(15, 25, 90, 150, null));
    var policy = UserPreferencesPersistenceAssembler.toDomain(stored).notificationPolicy();
    assertEquals(new NotificationPolicy(15, 25, 90, 150, null), policy);
    assertNull(policy.dailySummaryTime());
  }

  private static UserPreferencesPersistenceEntity entity(LocalTime start, LocalTime end) {
    return new UserPreferencesPersistenceEntity(
        "preferences",
        "user",
        TextSizeLevel.MEDIUM,
        false,
        false,
        false,
        true,
        start,
        end,
        List.of(),
        Instant.EPOCH);
  }
}
