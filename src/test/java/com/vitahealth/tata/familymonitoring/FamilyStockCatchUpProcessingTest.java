package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.InitializeFamilyMonitorCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.ProcessFamilyStockCatchUpCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.InitializeFamilyMonitorCommand;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.scheduling.FamilyStockCatchUpRecoveryScheduler;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises committed stock predating follow-up through independent resumable processing steps. */
@SpringBootTest
@ActiveProfiles("test")
class FamilyStockCatchUpProcessingTest {
  @Autowired ProcessFamilyStockCatchUpCommandHandler process;
  @MockitoSpyBean FamilyStockCatchUpRepository work;
  @Autowired JdbcTemplate jdbc;
  @Autowired IFamilyMonitorRepository monitors;
  @Autowired CareLinkRepository links;
  @Autowired MedicationRepository medications;
  @Autowired TreatmentContextFacade treatments;
  @Autowired InventoryCommandService inventories;
  @Autowired InventoryRepository inventoryRows;
  @Autowired PlatformTransactionManager transactions;
  @Autowired Clock clock;
  @Autowired InitializeFamilyMonitorCommandHandler initialize;
  @Autowired FamilyStockCatchUpRecoveryScheduler scheduler;

  private FamilyMonitor followUp(String adult, boolean revoke) {
    var now = clock.instant();
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            UUID.randomUUID().toString(),
            adult,
            LinkingCode.issue(code, now, Duration.ofMinutes(15)),
            now);
    link.accept(code, now);
    link.registerConsent(true, now);
    link.confirm(now);
    if (revoke) link.withdrawConsent(adult, now);
    links.save(link);
    initialize.handle(new InitializeFamilyMonitorCommand(link.id(), adult, link.caregiverId()));
    return monitors.findByCareLinkId(link.id()).orElseThrow();
  }

  private String medication(String adult, Integer quantity) {
    var medication =
        medications.save(Medication.register(adult, "Losartan", "Tablet", clock.instant()));
    if (quantity != null)
      inventories.registerInitialInventory(
          new RegisterInitialInventoryCommand(medication.id(), quantity, 5));
    return medication.id();
  }

  @Test
  void recoversPreexistingLowAndHealthyStockAndResumesOneMedicationAtATime() {
    var adult = UUID.randomUUID().toString();
    medication(adult, 3);
    medication(adult, 12);
    medication(adult, null);
    var monitor = followUp(adult, false);
    assertTrue(monitors.findById(monitor.getId()).orElseThrow().getLowStockNotices().isEmpty());
    assertTrue(process.handle(monitor.getId()));
    var cursor = work.find(monitor.getId()).orElseThrow().afterMedicationId();
    assertFalse(cursor.isEmpty());
    assertTrue(process.handle(monitor.getId()));
    assertTrue(work.find(monitor.getId()).orElseThrow().afterMedicationId().compareTo(cursor) > 0);
    assertTrue(process.handle(monitor.getId()));
    assertFalse(process.handle(monitor.getId()));
    assertFalse(process.handle(monitor.getId()));
    assertTrue(work.find(monitor.getId()).isEmpty());
    var stored = monitors.findById(monitor.getId()).orElseThrow();
    assertEquals(2, stored.getInventoryStockPositions().size());
    assertEquals(1, stored.getLowStockNotices().size());
    assertEquals(3, stored.getLowStockNotices().getFirst().getRemainingStock());
  }

  @Test
  void revokedConsentCompletesWorkWithoutProjectingStock() {
    var adult = UUID.randomUUID().toString();
    medication(adult, 2);
    var monitor = followUp(adult, true);
    assertFalse(process.handle(monitor.getId()));
    assertTrue(work.find(monitor.getId()).isEmpty());
    assertTrue(
        monitors.findById(monitor.getId()).orElseThrow().getInventoryStockPositions().isEmpty());
  }

  @Test
  void periodicRecoveryProcessesCreatedWorkAndConfirmationReplayDoesNotRecreateIt() {
    var adult = UUID.randomUUID().toString();
    medication(adult, 2);
    var monitor = followUp(adult, false);
    assertTrue(work.find(monitor.getId()).isPresent());
    scheduler.recover();
    assertTrue(work.find(monitor.getId()).isEmpty());
    assertEquals(
        2,
        monitors
            .findById(monitor.getId())
            .orElseThrow()
            .getLowStockNotices()
            .getFirst()
            .getRemainingStock());
    initialize.handle(
        new InitializeFamilyMonitorCommand(
            monitor.getCareLinkId(), adult, monitor.getFamiliarId()));
    assertTrue(work.find(monitor.getId()).isEmpty());
  }

  @Test
  void rolledBackInitializationLeavesNeitherMonitorNorWork() {
    var link = UUID.randomUUID().toString();
    var adult = UUID.randomUUID().toString();
    var caregiver = UUID.randomUUID().toString();
    var createdId = new java.util.concurrent.atomic.AtomicLong();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              initialize.handle(new InitializeFamilyMonitorCommand(link, adult, caregiver));
              var monitor = monitors.findByCareLinkId(link).orElseThrow();
              createdId.set(monitor.getId());
              assertTrue(work.find(monitor.getId()).isPresent());
              status.setRollbackOnly();
            });
    assertTrue(monitors.findByCareLinkId(link).isEmpty());
    assertTrue(work.find(createdId.get()).isEmpty());
  }

  @Test
  void failedCursorWriteRollsBackProjectionBacksOffAndDoesNotBlockAnotherRelationship() {
    var failingAdult = UUID.randomUUID().toString();
    medication(failingAdult, 2);
    var failing = followUp(failingAdult, false);
    var original = work.find(failing.getId()).orElseThrow();
    var healthyAdult = UUID.randomUUID().toString();
    medication(healthyAdult, 3);
    var healthy = followUp(healthyAdult, false);
    FamilyStockCatchUpRepository target = AopTestUtils.getUltimateTargetObject(work);
    doAnswer(
            invocation -> {
              FamilyStockCatchUp expected = invocation.getArgument(0);
              FamilyStockCatchUp replacement = invocation.getArgument(1);
              if (expected.monitorId() == failing.getId()
                  && !replacement.afterMedicationId().isEmpty())
                throw new IllegalStateException("controlled cursor persistence failure");
              return invocation.callRealMethod();
            })
        .when(target)
        .replace(any(FamilyStockCatchUp.class), any(FamilyStockCatchUp.class));
    try {
      scheduler.recover();
      var retained = work.find(failing.getId()).orElseThrow();
      assertEquals(original.afterMedicationId(), retained.afterMedicationId());
      assertEquals(original.requestedAt(), retained.requestedAt());
      assertEquals(1, retained.attempts());
      assertTrue(retained.nextAttemptAt().isAfter(original.nextAttemptAt()));
      var unchanged = monitors.findById(failing.getId()).orElseThrow();
      assertTrue(unchanged.getLowStockNotices().isEmpty());
      assertTrue(unchanged.getInventoryStockPositions().isEmpty());
      assertEquals(failing.getRevision(), unchanged.getRevision());
      assertTrue(work.find(healthy.getId()).isEmpty());
      assertEquals(
          3,
          monitors
              .findById(healthy.getId())
              .orElseThrow()
              .getLowStockNotices()
              .getFirst()
              .getRemainingStock());
      scheduler.recover();
      assertEquals(retained, work.find(failing.getId()).orElseThrow());
    } finally {
      doCallRealMethod()
          .when(target)
          .replace(any(FamilyStockCatchUp.class), any(FamilyStockCatchUp.class));
    }
    jdbc.update(
        "update family_stock_catchups set next_attempt_at = requested_at where family_monitor_id ="
            + " ?",
        failing.getId());
    scheduler.recover();
    assertTrue(work.find(failing.getId()).isEmpty());
    assertEquals(
        2,
        monitors
            .findById(failing.getId())
            .orElseThrow()
            .getLowStockNotices()
            .getFirst()
            .getRemainingStock());
  }

  @Test
  void concurrentWorkersAdvanceOneMedicationExactlyOnce() throws Exception {
    var adult = UUID.randomUUID().toString();
    medication(adult, 2);
    var monitor = followUp(adult, false);
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                return process.handle(monitor.getId());
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                return process.handle(monitor.getId());
              });
      start.countDown();
      var outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
      assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
    }
    assertTrue(work.find(monitor.getId()).isEmpty());
    var stored = monitors.findById(monitor.getId()).orElseThrow();
    assertEquals(1, stored.getInventoryStockPositions().size());
    assertEquals(1, stored.getLowStockNotices().size());
  }

  @Test
  void catchUpWaitsForFirstInventoryRegistrationAndKeepsItsCommittedStock() throws Exception {
    var adult = UUID.randomUUID().toString();
    var medicationId = medication(adult, null);
    var monitor = followUp(adult, false);
    var reserved = new CountDownLatch(1);
    var processorStarted = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var registration =
          workers.submit(
              () -> {
                new TransactionTemplate(transactions)
                    .executeWithoutResult(
                        status -> {
                          treatments.reserveMedication(medicationId);
                          reserved.countDown();
                          try {
                            assertTrue(processorStarted.await(10, TimeUnit.SECONDS));
                          } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(interrupted);
                          }
                          inventories.registerInitialInventory(
                              new RegisterInitialInventoryCommand(medicationId, 2, 5));
                        });
                return true;
              });
      assertTrue(reserved.await(10, TimeUnit.SECONDS));
      var catchingUp =
          workers.submit(
              () -> {
                processorStarted.countDown();
                return process.handle(monitor.getId());
              });
      assertTrue(registration.get(15, TimeUnit.SECONDS));
      assertTrue(catchingUp.get(15, TimeUnit.SECONDS));
    }
    assertFalse(process.handle(monitor.getId()));
    assertTrue(work.find(monitor.getId()).isEmpty());
    var stored = monitors.findById(monitor.getId()).orElseThrow();
    assertEquals(1, stored.getInventoryStockPositions().size());
    assertEquals(2, stored.getLowStockNotices().getFirst().getRemainingStock());
  }

  @Test
  void concurrentReplenishmentAndCatchUpKeepLatestHealthyRevision() throws Exception {
    var adult = UUID.randomUUID().toString();
    var medicationId = medication(adult, 2);
    var monitor = followUp(adult, false);
    var reserved = new CountDownLatch(1);
    var processorStarted = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var replenishment =
          workers.submit(
              () -> {
                new TransactionTemplate(transactions)
                    .executeWithoutResult(
                        status -> {
                          inventoryRows.findByMedicationIdForUpdate(medicationId).orElseThrow();
                          reserved.countDown();
                          try {
                            assertTrue(processorStarted.await(10, TimeUnit.SECONDS));
                          } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(interrupted);
                          }
                          inventories.registerReplenishment(
                              new RegisterReplenishmentCommand(medicationId, 10));
                        });
                return true;
              });
      assertTrue(reserved.await(10, TimeUnit.SECONDS));
      var catchingUp =
          workers.submit(
              () -> {
                processorStarted.countDown();
                return process.handle(monitor.getId());
              });
      assertTrue(replenishment.get(15, TimeUnit.SECONDS));
      assertTrue(catchingUp.get(15, TimeUnit.SECONDS));
    }
    assertFalse(process.handle(monitor.getId()));
    var source = inventoryRows.findByMedicationId(medicationId).orElseThrow();
    var stored = monitors.findById(monitor.getId()).orElseThrow();
    assertTrue(stored.getLowStockNotices().isEmpty());
    assertEquals(source.revision(), stored.getInventoryStockPositions().getFirst().revision());
    assertEquals(12, stored.getInventoryStockPositions().getFirst().remainingStock());
  }

  @Test
  void emptyMedicationSourceCompletesWithoutCreatingStockEvidence() {
    var monitor = followUp(UUID.randomUUID().toString(), false);
    assertFalse(process.handle(monitor.getId()));
    assertTrue(work.find(monitor.getId()).isEmpty());
    assertTrue(
        monitors.findById(monitor.getId()).orElseThrow().getInventoryStockPositions().isEmpty());
  }
}
