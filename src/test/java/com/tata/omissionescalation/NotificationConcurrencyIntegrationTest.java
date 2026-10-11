package com.tata.omissionescalation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tata.omissionescalation.application.internal.commandservices.EscalateOmissionCommandHandler;
import com.tata.omissionescalation.application.internal.commandservices.GenerateCaregiverAlertCommandHandler;
import com.tata.omissionescalation.application.internal.commandservices.SendReinforcedReminderCommandHandler;
import com.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.tata.omissionescalation.application.internal.outboundservices.INotificationPort;
import com.tata.omissionescalation.domain.factories.OmissionCaseFactory;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.tata.omissionescalation.domain.model.events.EscalationExecuted;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Overlaps real H2 transactions; only the first provider call pauses, so a missing lock fails. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationConcurrencyIntegrationTest {
  @Autowired IOmissionCaseRepository repository;
  @Autowired PlatformTransactionManager transactions;
  @Autowired SendReinforcedReminderCommandHandler reminders;
  @Autowired GenerateCaregiverAlertCommandHandler alerts;
  @Autowired EscalateOmissionCommandHandler escalations;
  @MockitoBean INotificationPort notifications;
  @MockitoBean IDomainEventPublisher events;

  @Test
  void concurrentRemindersSendAndRecordOnce() throws Exception {
    var omissionCase = storedCase(false);
    var gate = new FirstCallGate();
    when(notifications.sendReinforcedReminder(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              gate.holdFirst();
              return INotificationPort.NotificationResult.success();
            });
    overlap(gate, () -> reminders.handle(new SendReinforcedReminderCommand(omissionCase.getId())));
    verify(notifications, times(1)).sendReinforcedReminder(any(), any(), any());
    assertNotNull(read(omissionCase.getId()).getReinforcedReminderSentAt());
  }

  @Test
  void concurrentCaregiverAlertsPersistAndPublishOnce() throws Exception {
    var omissionCase = storedCase(true);
    var gate = new FirstCallGate();
    var now = Instant.now();
    when(notifications.sendCaregiverAlert(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              gate.holdFirst();
              return INotificationPort.NotificationResult.success();
            });
    overlap(
        gate, () -> alerts.handle(new GenerateCaregiverAlertCommand(omissionCase.getId(), now)));
    verify(notifications, times(1)).sendCaregiverAlert(any(), any(), any());
    verify(events, times(1)).publish(any());
    assertEquals(1, read(omissionCase.getId()).getAlerts().size());
  }

  @Test
  void concurrentEscalationsDoNotSkipAnAttentionLevel() throws Exception {
    var omissionCase = storedCase(true);
    var gate = new FirstCallGate();
    var now = Instant.now();
    doAnswer(
            invocation -> {
              gate.holdFirst();
              return null;
            })
        .when(events)
        .publish(any(EscalationExecuted.class));
    overlap(gate, () -> escalations.handle(new EscalateOmissionCommand(omissionCase.getId(), now)));
    var stored = read(omissionCase.getId());
    assertEquals(1, stored.getEscalations().size());
    assertEquals(1, stored.currentLevel().value());
    verify(events, times(1)).publish(any(EscalationExecuted.class));
  }

  private OmissionCase storedCase(boolean omitted) {
    return new TransactionTemplate(transactions)
        .execute(
            status -> {
              var now = Instant.now();
              var scheduled = now.minus(omitted ? Duration.ofHours(3) : Duration.ofMinutes(10));
              var value =
                  OmissionCaseFactory.createPending(
                  UUID.randomUUID().toString(),
                  UUID.randomUUID().toString(),
                      "Losartan 50 mg",
                      scheduled,
                      GracePeriod.startingAt(scheduled, Duration.ofMinutes(30)));
              if (omitted) value.markOmitted(now.minus(Duration.ofHours(2)));
              return repository.save(value);
            });
  }

  private OmissionCase read(Long id) {
    return new TransactionTemplate(transactions)
        .execute(status -> repository.findById(id).orElseThrow());
  }

  private static void overlap(FirstCallGate gate, Runnable action) throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(action);
      assertTrue(
          gate.entered.await(10, TimeUnit.SECONDS), "First transaction did not reach the effect");
      var secondStarted = new CountDownLatch(1);
      var second =
          executor.submit(
              () -> {
                secondStarted.countDown();
                action.run();
              });
      assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
      assertThrows(
          TimeoutException.class,
          () -> second.get(200, TimeUnit.MILLISECONDS),
          "Second transaction must wait for the case lock");
      gate.release.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    } finally {
      gate.release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  private static final class FirstCallGate {
    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);
    final AtomicBoolean first = new AtomicBoolean(true);

    void holdFirst() throws InterruptedException {
      if (!first.compareAndSet(true, false)) return;
      entered.countDown();
      if (!release.await(10, TimeUnit.SECONDS))
        throw new IllegalStateException("Timed out waiting for release");
    }
  }
}
