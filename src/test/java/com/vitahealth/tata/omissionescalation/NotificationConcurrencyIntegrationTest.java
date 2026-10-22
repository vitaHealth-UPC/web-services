package com.vitahealth.tata.omissionescalation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.omissionescalation.application.internal.commandservices.CloseOmissionCaseCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EscalateOmissionCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.GenerateCaregiverAlertCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.SendReinforcedReminderCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationDeviceTransport;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.CloseOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.SendReinforcedReminderCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.CaregiverAlertGenerated;
import com.vitahealth.tata.omissionescalation.domain.model.events.EscalationExecuted;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
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
  @Autowired CloseOmissionCaseCommandHandler closer;
  @MockitoBean NotificationDeviceTransport notifications;
  @MockitoBean IDomainEventPublisher events;
  @MockitoSpyBean NotificationIntentStore intentions;
  @Autowired NotificationIntentJpaRepository intentRows;

  @Test
  void concurrentRemindersQueueOneIntentWithoutRecordingDelivery() throws Exception {
    var omissionCase = storedCase(false);
    var gate = new FirstCallGate();
    doAnswer(
            invocation -> {
              gate.holdFirst();
              return invocation.callRealMethod();
            })
        .when((NotificationIntentStore) AopTestUtils.getUltimateTargetObject(intentions))
        .enqueue(any());
    overlap(gate, () -> reminders.handle(new SendReinforcedReminderCommand(omissionCase.getId())));
    var intent =
        intentRows
            .findBySourceCaseIdAndKind(omissionCase.getId(), NotificationKind.REINFORCED_REMINDER)
            .orElseThrow();
    assertNotNull(intent.getId());
    assertNull(read(omissionCase.getId()).getReinforcedReminderSentAt());
    verifyNoInteractions(notifications);
  }

  @Test
  void concurrentCaregiverAlertsPersistQueueAndPublishOnce() throws Exception {
    var source = storedCase(true);
    var gate = new FirstCallGate();
    var now = Instant.now();
    doAnswer(
            invocation -> {
              gate.holdFirst();
              return invocation.callRealMethod();
            })
        .when((NotificationIntentStore) AopTestUtils.getUltimateTargetObject(intentions))
        .enqueue(any());
    overlap(gate, () -> alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now)));
    var stored = read(source.getId());
    assertEquals(1, stored.getAlerts().size());
    assertEquals(AlertStatus.GENERATED, stored.getAlerts().getFirst().getStatus());
    assertNull(stored.getAlerts().getFirst().getSentAt());
    assertTrue(
        intentRows
            .findBySourceCaseIdAndKind(source.getId(), NotificationKind.CAREGIVER_ALERT)
            .isPresent());
    verify(events, times(1)).publish(any(CaregiverAlertGenerated.class));
    verifyNoInteractions(notifications);
  }

  @Test
  void repeatedAlertRequestPreservesOriginalBusinessEvidenceAndIntention() {
    var source = storedCase(true);
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now));
    var original = read(source.getId());
    var alert = original.getAlerts().getFirst();
    var intent =
        intentRows
            .findBySourceCaseIdAndKind(source.getId(), NotificationKind.CAREGIVER_ALERT)
            .orElseThrow();
    alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now.plusSeconds(60)));
    alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now.plusSeconds(120)));
    var repeated = read(source.getId());
    assertEquals(alert.getId(), repeated.getAlerts().getFirst().getId());
    assertEquals(alert.getGeneratedAt(), repeated.getAlerts().getFirst().getGeneratedAt());
    assertEquals(original.getOmittedAt(), repeated.getOmittedAt());
    assertEquals(
        intent.getId(),
        intentRows
            .findBySourceCaseIdAndKind(source.getId(), NotificationKind.CAREGIVER_ALERT)
            .orElseThrow()
            .getId());
    assertNull(repeated.getAlerts().getFirst().getSentAt());
    verify(events, times(1)).publish(any(CaregiverAlertGenerated.class));
    verifyNoInteractions(notifications);
  }

  @Test
  void concurrentRequestsForExistingFailedAlertRetainFailureEvidenceUntilActualDelivery()
      throws Exception {
    var source = storedCase(true);
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var value = repository.findByIdForUpdate(source.getId()).orElseThrow();
              value.addAlert(now);
              value.markAlertFailed("historical failure");
              repository.save(value);
            });
    var original = read(source.getId()).getAlerts().getFirst();
    var gate = new FirstCallGate();
    doAnswer(
            invocation -> {
              gate.holdFirst();
              return invocation.callRealMethod();
            })
        .when((NotificationIntentStore) AopTestUtils.getUltimateTargetObject(intentions))
        .enqueue(any());
    overlap(
        gate,
        () ->
            alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now.plusSeconds(60))));
    var stored = read(source.getId()).getAlerts().getFirst();
    assertEquals(original.getId(), stored.getId());
    assertEquals(AlertStatus.FAILED, stored.getStatus());
    assertEquals("historical failure", stored.getFailureReason());
    assertNull(stored.getSentAt());
    assertTrue(
        intentRows
            .findBySourceCaseIdAndKind(source.getId(), NotificationKind.CAREGIVER_ALERT)
            .isPresent());
    verifyNoInteractions(events, notifications);
  }

  @Test
  void aClosedCaseDoesNotQueueItsAlert() {
    var source = storedCase(true);
    var now = Instant.now();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var value = repository.findByIdForUpdate(source.getId()).orElseThrow();
              value.close(now);
              repository.save(value);
            });
    alerts.handle(new GenerateCaregiverAlertCommand(source.getId(), now.plusSeconds(60)));
    assertTrue(
        intentRows
            .findBySourceCaseIdAndKind(source.getId(), NotificationKind.CAREGIVER_ALERT)
            .isEmpty());
    verifyNoInteractions(events, notifications, intentions);
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

  @Test
  void standaloneCloseWaitsForEscalationAndPreservesItsCommittedHistory() throws Exception {
    var value = storedCase(true);
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var gate = new FirstCallGate();
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .executeWithoutResult(
                          status -> {
                            var locked = repository.findByIdForUpdate(value.getId()).orElseThrow();
                            locked.escalate("attention required", now);
                            repository.save(locked);
                            try {
                              gate.holdFirst();
                            } catch (InterruptedException failure) {
                              Thread.currentThread().interrupt();
                              throw new IllegalStateException(failure);
                            }
                          }));
      assertTrue(gate.entered.await(10, TimeUnit.SECONDS));
      var started = new CountDownLatch(1);
      var second =
          executor.submit(
              () -> {
                started.countDown();
                closer.handle(new CloseOmissionCaseCommand(value.getId(), now.plusSeconds(60)));
              });
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
      gate.release.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
      var stored = read(value.getId());
      assertEquals(OmissionCaseStatus.CLOSED, stored.getStatus());
      assertEquals(1, stored.currentLevel().value());
      assertEquals(1, stored.getEscalations().size());
      assertEquals("attention required", stored.getEscalations().getFirst().getReason());
      assertNotNull(stored.getEscalations().getFirst().getId());
      closer.handle(new CloseOmissionCaseCommand(value.getId(), now.plusSeconds(120)));
      assertEquals(now.plusSeconds(60), read(value.getId()).getClosedAt());
    } finally {
      gate.release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  private OmissionCase storedCase(boolean omitted) {
    return new TransactionTemplate(transactions)
        .execute(
            status -> {
              var now = Instant.now();
              var scheduled = now.minus(omitted ? Duration.ofHours(3) : Duration.ofMinutes(10));
              var value =
                  new OmissionCase(
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
