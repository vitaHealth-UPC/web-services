package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.omissionescalation.application.internal.commandservices.EvaluateGracePeriodCommandHandler;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies automatic unresolved handoff, source locking and definitive omission evidence. */
@SpringBootTest
@ActiveProfiles("test")
class AutomaticUnconfirmedIntakeIntegrationTest {

  @Autowired IntakeRepository intakes;
  @Autowired IOmissionCaseRepository omissionCases;
  @Autowired ReportUnconfirmedIntakesCommandHandler reportUnconfirmed;
  @Autowired EvaluateGracePeriodCommandHandler evaluateGracePeriod;
  @Autowired PlatformTransactionManager transactions;

  @Autowired ConfirmIntakeCommandService confirmation;

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
    var transaction = new TransactionTemplate(transactions);
    var started = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var attempt =
          transaction.execute(
              status -> {
                assertTrue(intakes.findByIdForConfirmation(intake.id()).isPresent());
                var future =
                    executor.submit(
                        () -> {
                          started.countDown();
                          return confirmation
                              .handle(
                                  new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH))
                              .toOptional()
                              .orElseThrow();
                        });
                try {
                  assertTrue(started.await(5, TimeUnit.SECONDS));
                  assertThrows(
                      TimeoutException.class, () -> future.get(200, TimeUnit.MILLISECONDS));
                } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                  throw new RuntimeException(interrupted);
                }
                return future;
              });
      assertNotNull(attempt);
      attempt.get(15, TimeUnit.SECONDS);
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
    assertNull(omissionCase.getReinforcedReminderSentAt());

    evaluateGracePeriod.handle(new EvaluateGracePeriodCommand(reportedAt.plusSeconds(5)));

    assertEquals(
        OmissionCaseStatus.OMITTED,
        omissionCases.findByIntakeId(intake.id()).orElseThrow().getStatus());
    assertEquals(IntakeStatus.OMITTED, intakes.findById(intake.id()).orElseThrow().status());
  }
}
