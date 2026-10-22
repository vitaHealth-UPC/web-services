package com.vitahealth.tata.accessibilitypreferences.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices.InitializeDefaultPreferencesCommandHandler;
import com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices.UserPreferencesMutationService;
import com.vitahealth.tata.accessibilitypreferences.application.internal.queryservices.GetUserPreferencesQueryHandler;
import com.vitahealth.tata.accessibilitypreferences.application.internal.queryservices.UserPreferencesQueryServiceImpl;
import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Proves public and cross-context reads share explicit-time defaults without persistence writes.
 */
class PreferencesReadBoundaryTest {
  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserPreferencesRepository repository = mock(UserPreferencesRepository.class);
  private final GetUserPreferencesQueryHandler read =
      new GetUserPreferencesQueryHandler(repository, CLOCK);
  private final UserPreferencesQueryServiceImpl projection =
      new UserPreferencesQueryServiceImpl(read);

  @Test
  void absentPreferencesUseApplicationTimeAndNeverPersistDuringAnyRead() {
    when(repository.findByUserId("adult")).thenReturn(Optional.empty());
    var defaults = read.handle(new GetUserPreferencesQuery(" adult "));
    var notifications = projection.getNotificationPreferences("adult");

    assertEquals("adult", defaults.userId());
    assertEquals(NOW, defaults.createdAt());
    assertTrue(defaults.voiceConfirmationEnabled());
    assertTrue(projection.isVoiceConfirmationEnabled("adult"));
    assertTrue(notifications.pushEnabled());
    assertFalse(notifications.smsEnabled());
    assertFalse(notifications.emailEnabled());
    assertNull(notifications.quietHoursStart());
    assertNull(notifications.quietHoursEnd());
    verify(repository, never()).save(any());
  }

  @Test
  void savedChoicesAndOvernightQuietHoursArePreservedByEveryRead() {
    var stored = UserPreferencesFactory.createDefaults("adult", NOW.minusSeconds(300));
    stored.setVoiceConfirmation(false);
    stored.setQuietHours(QuietHoursRange.between(LocalTime.of(22, 0), LocalTime.of(7, 0)));
    stored.updateChannels(
        List.of(
            new NotificationChannel(ChannelType.PUSH, false),
            new NotificationChannel(ChannelType.SMS, true),
            new NotificationChannel(ChannelType.EMAIL, true)));
    when(repository.findByUserId("adult")).thenReturn(Optional.of(stored));

    assertEquals(stored.id(), read.handle(new GetUserPreferencesQuery("adult")).id());
    assertFalse(projection.isVoiceConfirmationEnabled("adult"));
    var notifications = projection.getNotificationPreferences("adult");
    assertFalse(notifications.pushEnabled());
    assertTrue(notifications.smsEnabled());
    assertTrue(notifications.emailEnabled());
    assertEquals(LocalTime.of(22, 0), notifications.quietHoursStart());
    assertEquals(LocalTime.of(7, 0), notifications.quietHoursEnd());
    assertEquals(NOW.minusSeconds(300), stored.createdAt());
    verify(repository, never()).save(any());
  }

  @Test
  void missingOwnerIsRejectedBeforeAnyRepositoryLookup() {
    assertThrows(IllegalArgumentException.class, () -> new GetUserPreferencesQuery(null));
    assertThrows(IllegalArgumentException.class, () -> projection.getNotificationPreferences(" "));
    assertThrows(IllegalArgumentException.class, () -> projection.isVoiceConfirmationEnabled(""));
    verifyNoInteractions(repository);
  }

  @Test
  void explicitInitializationUsesApplicationTimeAndPreservesExistingState() {
    when(repository.findByUserIdForUpdate("adult")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var initialization =
        new InitializeDefaultPreferencesCommandHandler(
            new UserPreferencesMutationService(repository, user -> {}, CLOCK));
    var created = initialization.handle(new InitializeDefaultPreferencesCommand("adult"));
    assertEquals(NOW, created.createdAt());
    verify(repository).save(created);

    var existingRepository = mock(UserPreferencesRepository.class);
    when(existingRepository.findByUserIdForUpdate("adult")).thenReturn(Optional.of(created));
    var replay =
        new InitializeDefaultPreferencesCommandHandler(
                new UserPreferencesMutationService(existingRepository, user -> {}, CLOCK))
            .handle(new InitializeDefaultPreferencesCommand("adult"));
    assertSame(created, replay);
    verify(existingRepository, never()).save(any());
  }
}
