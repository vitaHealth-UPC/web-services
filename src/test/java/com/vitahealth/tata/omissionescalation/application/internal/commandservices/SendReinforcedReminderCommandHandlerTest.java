package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies durable reminder requests never fabricate sent evidence or swallow storage failures. */
@ExtendWith(MockitoExtension.class)
class SendReinforcedReminderCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");
  @Mock IOmissionCaseRepository repository;
  @Mock NotificationIntentStore intentions;

  private OmissionCase pendingCase() {
    return new OmissionCase(
        "intake-1",
        "adult-10",
        "Medication",
        NOW.minusSeconds(60),
        GracePeriod.startingAt(NOW.minusSeconds(60), Duration.ofMinutes(30)));
  }

  private SendReinforcedReminderCommandHandler handler() {
    return new SendReinforcedReminderCommandHandler(
        repository, intentions, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void pendingReminderQueuesWithoutMarkingOrSavingSource() {
    var source = pendingCase();
    when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(source));
    handler().handle(new SendReinforcedReminderCommand(7L));
    verify(intentions)
        .enqueue(new NotificationIntentRequest(7, NotificationKind.REINFORCED_REMINDER, NOW));
    assertThat(source.getReinforcedReminderSentAt()).isNull();
    verify(repository).findByIdForUpdate(7L);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void alreadySentReminderDoesNotQueueAgain() {
    var source = pendingCase();
    source.markReminderSent(NOW);
    when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(source));
    handler().handle(new SendReinforcedReminderCommand(7L));
    verifyNoInteractions(intentions);
  }

  @Test
  void resolvedSourceDoesNotQueueReminder() {
    var source = pendingCase();
    source.resolve();
    when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(source));
    handler().handle(new SendReinforcedReminderCommand(7L));
    verifyNoInteractions(intentions);
  }

  @Test
  void storageFailurePropagatesWithoutClaimingDelivery() {
    var source = pendingCase();
    when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(source));
    doThrow(new IllegalStateException("storage unavailable")).when(intentions).enqueue(any());
    assertThatThrownBy(() -> handler().handle(new SendReinforcedReminderCommand(7L)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(source.getReinforcedReminderSentAt()).isNull();
  }
}
