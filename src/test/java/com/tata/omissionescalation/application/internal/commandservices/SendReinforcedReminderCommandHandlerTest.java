package com.tata.omissionescalation.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tata.omissionescalation.application.internal.outboundservices.INotificationPort;
import com.tata.omissionescalation.domain.factories.OmissionCaseFactory;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendReinforcedReminderCommandHandlerTest {

  private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

  @Mock IOmissionCaseRepository repository;
  @Mock INotificationPort notifications;
  @Mock INotificationPreferencesPort preferences;

  private OmissionCase pendingCase() {
    return OmissionCaseFactory.createPending(
        "intake-1",
        "adult-10",
        "Losartan 50 mg",
        NOW.minusSeconds(60),
        GracePeriod.startingAt(NOW.minusSeconds(60), Duration.ofMinutes(30)));
  }

  private SendReinforcedReminderCommandHandler handler() {
    return new SendReinforcedReminderCommandHandler(
        repository,
        notifications,
        preferences,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void enabledPushOutsideQuietHours_marksReminderAsSent() {
    OmissionCase omissionCase = pendingCase();
    when(repository.findById(7L)).thenReturn(Optional.of(omissionCase));
    when(preferences.getPreferences("adult-10")).thenReturn(NotificationPreferences.defaults());
    when(notifications.sendReinforcedReminder(any(), any(), any()))
        .thenReturn(INotificationPort.NotificationResult.success());

    handler().handle(new SendReinforcedReminderCommand(7L));

    assertThat(omissionCase.getReinforcedReminderSentAt()).isEqualTo(NOW);
    verify(repository).save(omissionCase);
  }

  @Test
  void disabledPush_skipsDeliveryAndKeepsReminderPending() {
    OmissionCase omissionCase = pendingCase();
    when(repository.findById(7L)).thenReturn(Optional.of(omissionCase));
    when(preferences.getPreferences("adult-10"))
        .thenReturn(new NotificationPreferences(false, null, null));

    handler().handle(new SendReinforcedReminderCommand(7L));

    assertThat(omissionCase.getReinforcedReminderSentAt()).isNull();
    verify(notifications, never()).sendReinforcedReminder(any(), any(), any());
    verify(repository, never()).save(any());
  }

  @Test
  void quietHours_skipDelivery() {
    OmissionCase omissionCase = pendingCase();
    when(repository.findById(7L)).thenReturn(Optional.of(omissionCase));
    when(preferences.getPreferences("adult-10"))
        .thenReturn(new NotificationPreferences(
            true,
            LocalTime.of(7, 0),
            LocalTime.of(9, 0)));

    handler().handle(new SendReinforcedReminderCommand(7L));

    assertThat(omissionCase.getReinforcedReminderSentAt()).isNull();
    verify(notifications, never()).sendReinforcedReminder(any(), any(), any());
  }

  @Test
  void providerFailure_doesNotMarkReminderAsSent() {
    OmissionCase omissionCase = pendingCase();
    when(repository.findById(7L)).thenReturn(Optional.of(omissionCase));
    when(preferences.getPreferences("adult-10")).thenReturn(NotificationPreferences.defaults());
    when(notifications.sendReinforcedReminder(any(), any(), any()))
        .thenReturn(INotificationPort.NotificationResult.failure("provider unavailable"));

    handler().handle(new SendReinforcedReminderCommand(7L));

    assertThat(omissionCase.getReinforcedReminderSentAt()).isNull();
    verify(repository, never()).save(any());
  }
}
