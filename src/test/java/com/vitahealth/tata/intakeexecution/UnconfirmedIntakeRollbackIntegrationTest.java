package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakeCommandHandler;
import com.vitahealth.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeUnconfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Proves source-marker and omission-case rollback permits exactly one later successful handoff. */
@SpringBootTest
@ActiveProfiles("test")
class UnconfirmedIntakeRollbackIntegrationTest {
  @Autowired IntakeRepository intakes;
  @Autowired IOmissionCaseRepository omissions;
  @Autowired ReportUnconfirmedIntakesCommandHandler reporting;
  @Autowired ReportUnconfirmedIntakeCommandHandler singleReporting;
  @Autowired IntakeUnconfirmedEventPublisher publisher;
  @Autowired SelectiveFailure consumer;
  @Autowired IntakeJpaRepository storage;
  @Autowired PlatformTransactionManager transactions;

  @Test
  void failedFirstCandidateDoesNotBlockMoreThanTwoPagesAndCanBeRetried() {
    var now = Instant.now();
    var scheduled = now.minusSeconds(300);
    var sources = new ArrayList<Intake>();
    for (int index = 0; index < 205; index++) {
      sources.add(
          Intake.createScheduled(
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              new MedicationSnapshot("Losartán", "1 comprimido", ""),
              scheduled,
              scheduled.minusSeconds(60)));
    }
    var saved = intakes.saveAll(sources);
    var failed = saved.stream().map(Intake::id).min(String::compareTo).orElseThrow();
    consumer.target = failed;
    consumer.observedMarker = false;
    consumer.observedCase = false;
    var command = new ReportUnconfirmedIntakesCommand(now, now);
    try {
      assertEquals(204, reporting.handle(command));
      assertTrue(consumer.observedMarker);
      assertTrue(consumer.observedCase);
      assertNull(intakes.findById(failed).orElseThrow().unconfirmedReportedAt());
      assertTrue(omissions.findByIntakeId(failed).isEmpty());
      for (var source : saved) {
        if (!source.id().equals(failed)) {
          assertTrue(intakes.findById(source.id()).orElseThrow().unconfirmedReportedAt() != null);
          assertTrue(omissions.findByIntakeId(source.id()).isPresent());
        }
      }
    } finally {
      consumer.target = null;
    }
    assertEquals(1, reporting.handle(command));
    assertEquals(0, reporting.handle(command));
  }

  @Test
  void malformedSnapshotDoesNotPreventAnotherSourceFromBeingReported() {
    var now = Instant.now();
    var malformed = UUID.randomUUID().toString();
    storage.saveAndFlush(
        new IntakePersistenceEntity(
            malformed,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "",
            "1 comprimido",
            "",
            now.minusSeconds(600),
            IntakeStatus.PENDING,
            now.minusSeconds(900)));
    var valid =
        intakes
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        now.minusSeconds(300),
                        now.minusSeconds(600))))
            .getFirst();
    try {
      assertEquals(1, reporting.handle(new ReportUnconfirmedIntakesCommand(now, now)));
      assertNull(storage.findById(malformed).orElseThrow().getUnconfirmedReportedAt());
      assertTrue(omissions.findByIntakeId(malformed).isEmpty());
      assertTrue(omissions.findByIntakeId(valid.id()).isPresent());
    } finally {
      storage.deleteById(malformed);
    }
  }

  @Test
  void ambientRollbackCannotUndoIndependentlyCommittedHandoffs() {
    var now = Instant.now();
    var intake =
        intakes
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        now.minusSeconds(300),
                        now.minusSeconds(600))))
            .getFirst();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              assertEquals(1, reporting.handle(new ReportUnconfirmedIntakesCommand(now, now)));
              status.setRollbackOnly();
            });
    assertTrue(intakes.findById(intake.id()).orElseThrow().unconfirmedReportedAt() != null);
    assertTrue(omissions.findByIntakeId(intake.id()).isPresent());
  }

  @Test
  void competingEvaluationsWaitForTheSourceAndPublishOnlyOnce() throws Exception {
    var now = Instant.now();
    var source =
        intakes
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        now.minusSeconds(300),
                        now.minusSeconds(600))))
            .getFirst();
    consumer.entered = new CountDownLatch(1);
    consumer.release = new CountDownLatch(1);
    consumer.blockedEvents.set(0);
    consumer.blockedTarget = source.id();
    var secondStarted = new CountDownLatch(1);
    var command = new ReportUnconfirmedIntakesCommand(now, now);
    try (var executor = Executors.newFixedThreadPool(2)) {
      try {
        var first = executor.submit(() -> reporting.handle(command));
        assertTrue(consumer.entered.await(10, TimeUnit.SECONDS));
        var second =
            executor.submit(
                () -> {
                  secondStarted.countDown();
                  return reporting.handle(command);
                });
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
        consumer.release.countDown();
        assertEquals(1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
        assertEquals(1, consumer.blockedEvents.get());
        assertTrue(omissions.findByIntakeId(source.id()).isPresent());
      } finally {
        consumer.release.countDown();
        consumer.blockedTarget = null;
      }
    }
  }

  @Test
  void failedConsumerRestoresUnreportedEvidenceAndRetryCreatesOneCase() {
    var now = Instant.now();
    var intake =
        intakes
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        now.minusSeconds(300),
                        now.minusSeconds(600))))
            .getFirst();
    var command = new ReportUnconfirmedIntakesCommand(now, now);
    consumer.target = intake.id();
    consumer.observedMarker = false;
    consumer.observedCase = false;
    try {
      assertThrows(
          IllegalStateException.class,
          () -> singleReporting.handle(new ReportUnconfirmedIntakeCommand(intake.id(), now, now)));
      assertTrue(consumer.observedMarker);
      assertTrue(consumer.observedCase);
      var restored = intakes.findById(intake.id()).orElseThrow();
      assertEquals(IntakeStatus.PENDING, restored.status());
      assertNull(restored.unconfirmedReportedAt());
      assertNull(restored.confirmedAt());
      assertTrue(omissions.findByIntakeId(intake.id()).isEmpty());
    } finally {
      consumer.target = null;
    }
    assertEquals(1, reporting.handle(command));
    var caseId = omissions.findByIntakeId(intake.id()).orElseThrow().getId();
    assertTrue(intakes.findById(intake.id()).orElseThrow().unconfirmedReportedAt() != null);
    assertEquals(0, reporting.handle(command));
    assertEquals(caseId, omissions.findByIntakeId(intake.id()).orElseThrow().getId());
  }

  @Test
  void sourceReservationRequiresTheReportingTransaction() {
    assertThrows(
        IllegalTransactionStateException.class,
        () -> intakes.findByIdForConfirmation(UUID.randomUUID().toString()));
  }

  @Test
  void publicationRequiresAnExistingSourceTransaction() {
    assertThrows(
        IllegalTransactionStateException.class,
        () ->
            publisher.publish(new IntakeUnconfirmed("intake", "adult", "Losartán", Instant.now())));
  }

  /** Observes the real source and downstream writes before deliberately rejecting one handoff. */
  static class SelectiveFailure {
    final IntakeRepository intakes;
    final IOmissionCaseRepository omissions;
    String target;
    boolean observedMarker;
    boolean observedCase;
    volatile String blockedTarget;
    volatile CountDownLatch entered;
    volatile CountDownLatch release;
    final AtomicInteger blockedEvents = new AtomicInteger();

    SelectiveFailure(IntakeRepository intakes, IOmissionCaseRepository omissions) {
      this.intakes = intakes;
      this.omissions = omissions;
    }

    @EventListener
    @Order(Integer.MAX_VALUE)
    void on(IntakeUnconfirmed event) {
      if (event.intakeId().equals(blockedTarget)) {
        blockedEvents.incrementAndGet();
        entered.countDown();
        try {
          if (!release.await(10, TimeUnit.SECONDS))
            throw new IllegalStateException("test handoff release timed out");
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("test handoff interrupted", interrupted);
        }
      }
      if (event.intakeId().equals(target)) {
        observedMarker =
            intakes.findById(event.intakeId()).orElseThrow().unconfirmedReportedAt() != null;
        observedCase = omissions.findByIntakeId(event.intakeId()).isPresent();
        throw new IllegalStateException("simulated handoff consumer failure");
      }
    }
  }

  /** Installs the failing observer only in this test context. */
  @TestConfiguration
  static class Consumers {
    @Bean
    SelectiveFailure selectiveFailure(IntakeRepository intakes, IOmissionCaseRepository omissions) {
      return new SelectiveFailure(intakes, omissions);
    }
  }
}
