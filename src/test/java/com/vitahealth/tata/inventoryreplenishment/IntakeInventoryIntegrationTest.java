package com.vitahealth.tata.inventoryreplenishment;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.*;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.scheduling.InventoryConsumptionRecoveryScheduler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class IntakeInventoryIntegrationTest {
  @Autowired ConfirmIntakeCommandHandler confirmations;
  @Autowired IntakeRepository intakes;

  @Autowired
  com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository medications;

  @Autowired InventoryCommandService commands;
  @Autowired InventoryRepository inventories;
  @Autowired InventoryConsumptionRequestRepository requests;
  @Autowired InventoryConsumptionRecoveryScheduler scheduler;
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationEventPublisher events;
  @Autowired PlatformTransactionManager transactionManager;

  private Intake intake() {
    var now = Instant.now();
    var adultId = UUID.randomUUID().toString();
    var medication =
        medications.save(
            com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication.register(
                adultId, "Losartan", "Tablet", now));
    var intake =
        Intake.createScheduled(
            UUID.randomUUID().toString(),
            medication.id(),
            adultId,
            new MedicationSnapshot("Losartan", "1 tablet", "With water"),
            now.plusSeconds(60),
            now);
    return intakes.saveAll(List.of(intake)).getFirst();
  }

  private void publish(IntakeConfirmed event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> events.publishEvent(event));
  }

  @Test
  void committedConfirmationAndRetriesConsumeOneUnit() {
    var intake = intake();
    commands.registerInitialInventory(
        new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
    confirmations.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH));
    confirmations.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.VOICE));
    publish(
        new IntakeConfirmed(
            intake.id(),
            intake.medicationId(),
            intake.olderAdultId(),
            intakes.findById(intake.id()).orElseThrow().confirmedAt()));
    assertEquals(
        9, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
    assertTrue(inventories.hasConsumed(intake.id()));
  }

  @Test
  void rolledBackConfirmationEventDoesNotConsumeStock() {
    var intake = intake();
    commands.registerInitialInventory(
        new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              var source = intakes.findByIdForConfirmation(intake.id()).orElseThrow();
              source.confirm(
                  Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                  ConfirmationChannel.TOUCH);
              intakes.saveAll(List.of(source));
              events.publishEvent(
                  new IntakeConfirmed(
                      source.id(),
                      source.medicationId(),
                      source.olderAdultId(),
                      source.confirmedAt()));
              status.setRollbackOnly();
            });
    assertEquals(
        10, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
    assertFalse(inventories.hasConsumed(intake.id()));
  }

  @Test
  void simultaneousRedeliveryConsumesOneUnit() throws Exception {
    var intake = intake();
    commands.registerInitialInventory(
        new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
    var confirmed = intakes.findById(intake.id()).orElseThrow();
    confirmed.confirm(
        Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), ConfirmationChannel.TOUCH);
    intakes.saveAll(List.of(confirmed));
    var event =
        new IntakeConfirmed(
            confirmed.id(),
            confirmed.medicationId(),
            confirmed.olderAdultId(),
            confirmed.confirmedAt());
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                start.await();
                publish(event);
                return true;
              });
      var second =
          executor.submit(
              () -> {
                start.await();
                publish(event);
                return true;
              });
      start.countDown();
      first.get(20, TimeUnit.SECONDS);
      second.get(20, TimeUnit.SECONDS);
    }
    assertEquals(
        9, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
    assertTrue(inventories.hasConsumed(intake.id()));
  }

  @Test
  void missingOrEmptyInventoryDoesNotUndoConfirmation() {
    var missing = intake();
    confirmations.handle(new ConfirmIntakeCommand(missing.id(), ConfirmationChannel.TOUCH));
    assertEquals(IntakeStatus.CONFIRMED, intakes.findById(missing.id()).orElseThrow().status());
    var empty = intake();
    commands.registerInitialInventory(
        new RegisterInitialInventoryCommand(empty.medicationId(), 1, 3));
    commands.consumeUnit(
        new com.vitahealth.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand(
            empty.medicationId(), UUID.randomUUID().toString()));
    confirmations.handle(new ConfirmIntakeCommand(empty.id(), ConfirmationChannel.TOUCH));
    assertEquals(IntakeStatus.CONFIRMED, intakes.findById(empty.id()).orElseThrow().status());
    assertEquals(
        0, inventories.findByMedicationId(empty.medicationId()).orElseThrow().remainingStock());
    assertFalse(inventories.hasConsumed(empty.id()));
  }

  @Test
  void schedulerRecoversCommittedConfirmationAfterInventoryRegistration() {
    var source = intake();
    confirmations.handle(new ConfirmIntakeCommand(source.id(), ConfirmationChannel.TOUCH));
    var pending = requests.findByIntakeId(source.id()).orElseThrow();
    assertEquals(1, pending.attempts());
    assertTrue(pending.nextAttemptAt().isAfter(pending.createdAt()));
    assertFalse(inventories.hasConsumed(source.id()));
    assertEquals(IntakeStatus.CONFIRMED, intakes.findById(source.id()).orElseThrow().status());
    commands.registerInitialInventory(
        new RegisterInitialInventoryCommand(source.medicationId(), 10, 3));
    // Make persisted intent due deterministically without waiting for the production backoff.
    assertEquals(
        1,
        jdbc.update(
            "update inventory_consumption_requests set next_attempt_at = created_at where intake_id"
                + " = ?",
            source.id()));
    scheduler.recover();
    scheduler.recover();
    assertEquals(
        9, inventories.findByMedicationId(source.medicationId()).orElseThrow().remainingStock());
    assertTrue(inventories.hasConsumed(source.id()));
    assertTrue(requests.findByIntakeId(source.id()).isEmpty());
  }
}
