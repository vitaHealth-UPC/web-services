package com.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.*;

import com.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AutomaticUnconfirmedIntakeIntegrationTest {

  @Autowired IntakeRepository intakes;
  @Autowired IOmissionCaseRepository omissionCases;
  @Autowired ReportUnconfirmedIntakesCommandHandler reportUnconfirmed;
  @Autowired EvaluateGracePeriodCommandHandler evaluateGracePeriod;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

  @Autowired
  com.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService confirmation;

  @Test
  void schedulerCandidatesKeepTheSourceLockUntilCommit() throws Exception {
    var scheduledAt = Instant.now().minusSeconds(600);
    var intake =
        intakes
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartan", "1 tableta", "Con agua"),
                        scheduledAt,
                        scheduledAt.minusSeconds(60))))
            .getFirst();
    var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
    var started = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var attempt =
          transaction.execute(
              status -> {
                assertTrue(
                    intakes.findUnreportedPendingDue(Instant.now()).stream()
                        .anyMatch(candidate -> candidate.id().equals(intake.id())));
                var future =
                    executor.submit(
                        () -> {
                          started.countDown();
                          return confirmation.handle(
                              new com.tata.intakeexecution.domain.model.commands
                                  .ConfirmIntakeCommand(
                                  intake.id(),
                                  com.tata.intakeexecution.domain.model.valueobjects
                                      .ConfirmationChannel.TOUCH));
                        });
                try {
                  assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
                  assertThrows(
                      java.util.concurrent.TimeoutException.class,
                      () -> future.get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
                } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                  throw new RuntimeException(interrupted);
                }
                return future;
              });
      assertNotNull(attempt);
      attempt.get(15, java.util.concurrent.TimeUnit.SECONDS);
    }
    assertEquals(IntakeStatus.LATE, intakes.findById(intake.id()).orElseThrow().status());
    reportUnconfirmed.handle(new ReportUnconfirmedIntakesCommand(Instant.now(), Instant.now()));
    assertTrue(omissionCases.findByIntakeId(intake.id()).isEmpty());
  }

  @Test
  void unresolvedIntakeIsReportedOnceAndBecomesDefinitivelyOmitted() {
    var scheduledAt = Instant.now().minusSeconds(1800);
    var intake =
        Intake.createScheduled(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new MedicationSnapshot("Losartan 50 mg", "1 tablet", "With water"),
            scheduledAt,
            scheduledAt.minusSeconds(60));
    intake = intakes.saveAll(List.of(intake)).getFirst();

    var reportedAt = Instant.now();

    assertEquals(
        1, reportUnconfirmed.handle(new ReportUnconfirmedIntakesCommand(reportedAt, reportedAt)));
    assertEquals(
        0,
        reportUnconfirmed.handle(
            new ReportUnconfirmedIntakesCommand(reportedAt, reportedAt.plusSeconds(1))));

    var reportedIntake = intakes.findById(intake.id()).orElseThrow();
    assertNotNull(reportedIntake.unconfirmedReportedAt());

    var omissionCase = omissionCases.findByIntakeId(intake.id()).orElseThrow();
    assertEquals(OmissionCaseStatus.PENDING, omissionCase.getStatus());
    assertNotNull(omissionCase.getReinforcedReminderSentAt());

    evaluateGracePeriod.handle(new EvaluateGracePeriodCommand(reportedAt.plusSeconds(5)));

    assertEquals(
        OmissionCaseStatus.OMITTED,
        omissionCases.findByIntakeId(intake.id()).orElseThrow().getStatus());
    assertEquals(IntakeStatus.OMITTED, intakes.findById(intake.id()).orElseThrow().status());
  }
}
