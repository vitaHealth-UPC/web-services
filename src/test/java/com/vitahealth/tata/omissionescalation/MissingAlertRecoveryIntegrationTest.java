package com.vitahealth.tata.omissionescalation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.GenerateCaregiverAlertCommandHandler;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationDeviceTransport;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.CaregiverAlertGenerated;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises missing-alert recovery from committed omission evidence across evaluator cycles. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class MissingAlertRecoveryIntegrationTest {
  @Autowired IOmissionCaseRepository repository;
  @Autowired JdbcTemplate jdbc;
  @Autowired OmissionCaseJpaRepository rows;
  @Autowired NotificationIntentJpaRepository intentRows;
  private final List<Long> ownedIds = new ArrayList<>();

  @AfterEach
  void removeOwnedRows() {
    for (Long id : ownedIds) {
      intentRows
          .findBySourceCaseIdAndKind(id, NotificationKind.CAREGIVER_ALERT)
          .ifPresent(intentRows::delete);
      intentRows
          .findBySourceCaseIdAndKind(id, NotificationKind.REINFORCED_REMINDER)
          .ifPresent(intentRows::delete);
      rows.deleteById(id);
    }
  }

  @Autowired PlatformTransactionManager transactions;
  @Autowired EvaluateGracePeriodCommandHandler evaluator;
  @MockitoSpyBean GenerateCaregiverAlertCommandHandler alerts;
  @MockitoSpyBean IntakeOmissionPort intakePort;
  @MockitoBean NotificationDeviceTransport notifications;
  @MockitoBean IDomainEventPublisher events;

  @Test
  void aFailedCreationDoesNotStarveLaterCasesAndNextCycleRecoversOnlyMissingAlert(
      CapturedOutput output) {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var failed = omitted(now);
    var healthy = omitted(now);
    var failOnce = new AtomicBoolean(true);
    doAnswer(
            invocation -> {
              GenerateCaregiverAlertCommand command = invocation.getArgument(0);
              if (command.omissionCaseId().equals(failed.getId()) && failOnce.getAndSet(false)) {
                throw new IllegalStateException("private-token-canary private-medication-canary");
              }
              return invocation.callRealMethod();
            })
        .when(alerts)
        .handle(any());

    evaluator.handle(new EvaluateGracePeriodCommand(now));
    assertTrue(read(failed.getId()).getAlerts().isEmpty());
    assertEquals(1, read(healthy.getId()).getAlerts().size());
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    var recovered = read(failed.getId());
    assertEquals(1, recovered.getAlerts().size());
    assertEquals(now, recovered.getOmittedAt());
    assertEquals(0, recovered.currentLevel().value());
    verifyNoInteractions(notifications);
    for (Long id : ownedIds)
      if (!read(id).getAlerts().isEmpty())
        assertTrue(
            intentRows.findBySourceCaseIdAndKind(id, NotificationKind.CAREGIVER_ALERT).isPresent());
    verify(events, times(2)).publish(any(CaregiverAlertGenerated.class));
    assertTrue(output.getAll().contains("category=CASE_EVALUATION_FAILED"));
    assertFalse(output.getAll().contains("private-token-canary"));
    assertFalse(output.getAll().contains("private-medication-canary"));
  }

  @Test
  void failedFirstReferenceDoesNotStarveRecoveryBeyondOneHundredRows() {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var failed = omitted(now);
    var others = new ArrayList<Long>();
    for (int index = 0; index < 104; index++) others.add(omitted(now).getId());
    doAnswer(
            invocation -> {
              GenerateCaregiverAlertCommand command = invocation.getArgument(0);
              if (command.omissionCaseId().equals(failed.getId())) {
                throw new IllegalStateException("simulated persistent creation failure");
              }
              return invocation.callRealMethod();
            })
        .when(alerts)
        .handle(any());
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    assertTrue(read(failed.getId()).getAlerts().isEmpty());
    for (var id : others) assertEquals(1, read(id).getAlerts().size());
    assertEquals(List.of(failed.getId()), repository.findActiveCaseIdsWithoutAlertAfter(0, 100));
    verifyNoInteractions(notifications);
    for (Long id : ownedIds)
      if (!read(id).getAlerts().isEmpty())
        assertTrue(
            intentRows.findBySourceCaseIdAndKind(id, NotificationKind.CAREGIVER_ALERT).isPresent());
    verify(events, times(104)).publish(any(CaregiverAlertGenerated.class));
  }

  @Test
  void omissionCommitSurvivesFailureAtAlertEntryAndNextEvaluationRecoversIt() {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var sourceId = UUID.randomUUID().toString();
    var pending =
        new TransactionTemplate(transactions)
            .execute(
                status ->
                    repository.save(
                        new OmissionCase(
                            sourceId,
                            UUID.randomUUID().toString(),
                            "Losartan",
                            now.minusSeconds(3600),
                            GracePeriod.startingAt(
                                now.minusSeconds(3600), Duration.ofMinutes(30)))));
    ownedIds.add(pending.getId());
    doReturn(true).when(intakePort).lockPendingIntake(sourceId);
    var failOnce = new AtomicBoolean(true);
    doAnswer(
            invocation -> {
              GenerateCaregiverAlertCommand command = invocation.getArgument(0);
              if (command.omissionCaseId().equals(pending.getId()) && failOnce.getAndSet(false)) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                var independentRead = new TransactionTemplate(transactions);
                independentRead.setPropagationBehavior(
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                var committed =
                    independentRead.execute(
                        status -> repository.findById(pending.getId()).orElseThrow());
                assertEquals(OmissionCaseStatus.OMITTED, committed.getStatus());
                assertEquals(now, committed.getOmittedAt());
                assertTrue(committed.getAlerts().isEmpty());
                throw new IllegalStateException("simulated failure after omission commit");
              }
              return invocation.callRealMethod();
            })
        .when(alerts)
        .handle(any());
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    assertFalse(failOnce.get());
    assertTrue(read(pending.getId()).getAlerts().isEmpty());
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    evaluator.handle(new EvaluateGracePeriodCommand(now));
    var recovered = read(pending.getId());
    assertEquals(now, recovered.getOmittedAt());
    assertEquals(1, recovered.getAlerts().size());
    assertEquals(0, recovered.currentLevel().value());
    verify(events, times(1)).publish(any(IntakeOmitted.class));
    verify(events, times(1)).publish(any(CaregiverAlertGenerated.class));
    verifyNoInteractions(notifications);
    for (Long id : ownedIds)
      if (!read(id).getAlerts().isEmpty())
        assertTrue(
            intentRows.findBySourceCaseIdAndKind(id, NotificationKind.CAREGIVER_ALERT).isPresent());
  }

  @Test
  void corruptActiveHistoryDoesNotPreventHealthyCaseRecoveryAndEscalation() {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var corrupt = omitted(now);
    var healthy = omitted(now);
    jdbc.update(
        "update omission_cases set closed_at = ? where id = ?",
        Timestamp.from(now),
        corrupt.getId());
    var before = jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId());
    evaluator.handle(new EvaluateGracePeriodCommand(now.plusSeconds(1860)));
    var recovered = read(healthy.getId());
    assertEquals(1, recovered.getAlerts().size());
    assertEquals(1, recovered.currentLevel().value());
    assertEquals(
        before, jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId()));
    verifyNoInteractions(notifications);
    assertTrue(
        intentRows
            .findBySourceCaseIdAndKind(healthy.getId(), NotificationKind.CAREGIVER_ALERT)
            .isPresent());
  }

  @Test
  void ambientCallerRollbackDoesNotUndoHealthyWorkOrJoinFailedCaseTransactions() {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var corrupt = omitted(now);
    var healthy = omitted(now);
    jdbc.update(
        "update omission_cases set closed_at = ? where id = ?",
        Timestamp.from(now),
        corrupt.getId());
    var before = jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId());

    new TransactionTemplate(transactions)
        .executeWithoutResult(
            caller -> {
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              evaluator.handle(new EvaluateGracePeriodCommand(now.plusSeconds(1860)));
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              assertFalse(caller.isRollbackOnly());
              caller.setRollbackOnly();
            });

    var committed = read(healthy.getId());
    assertEquals(1, committed.getAlerts().size());
    assertEquals(1, committed.currentLevel().value());
    assertTrue(
        intentRows
            .findBySourceCaseIdAndKind(healthy.getId(), NotificationKind.CAREGIVER_ALERT)
            .isPresent());
    assertEquals(
        before, jdbc.queryForMap("select * from omission_cases where id = ?", corrupt.getId()));
    verifyNoInteractions(notifications);
  }

  @Test
  void referenceQueryHonorsCursorLimitAndExcludesClosedCases() {
    var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
    var first = omitted(now);
    var second = omitted(now);
    var closed = omitted(now);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var value = repository.findByIdForUpdate(closed.getId()).orElseThrow();
              value.close(now);
              repository.save(value);
            });
    assertEquals(
        List.of(first.getId()),
        repository.findActiveCaseIdsWithoutAlertAfter(first.getId() - 1, 1));
    assertEquals(
        List.of(second.getId()), repository.findActiveCaseIdsWithoutAlertAfter(first.getId(), 1));
    assertTrue(repository.findActiveCaseIdsWithoutAlertAfter(second.getId(), 100).isEmpty());
    var invalid =
        assertThrows(
            InvalidDataAccessApiUsageException.class,
            () -> repository.findActiveCaseIdsWithoutAlertAfter(0, 101));
    assertInstanceOf(IllegalArgumentException.class, invalid.getCause());
  }

  private OmissionCase omitted(Instant now) {
    var saved =
        new TransactionTemplate(transactions)
            .execute(
                status -> {
                  var scheduled = now.minusSeconds(3600);
                  var value =
                      new OmissionCase(
                          UUID.randomUUID().toString(),
                          UUID.randomUUID().toString(),
                          "Losartan",
                          scheduled,
                          GracePeriod.startingAt(scheduled, Duration.ofMinutes(30)));
                  value.markOmitted(now);
                  return repository.save(value);
                });
    ownedIds.add(saved.getId());
    return saved;
  }

  private OmissionCase read(Long id) {
    return new TransactionTemplate(transactions)
        .execute(status -> repository.findById(id).orElseThrow());
  }
}
