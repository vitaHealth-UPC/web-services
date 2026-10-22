package com.vitahealth.tata.inventoryreplenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.ProcessInventoryConsumptionCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RecoverInventoryConsumptionsCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RequestInventoryConsumptionCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies decrement, marker and intent completion share one independent real transaction. */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConsumptionProcessingIntegrationTest {
  @Autowired ProcessInventoryConsumptionCommandHandler process;
  @Autowired RecoverInventoryConsumptionsCommandHandler recovery;
  @Autowired RequestInventoryConsumptionCommandHandler enqueue;
  @MockitoSpyBean InventoryConsumptionRequestRepository requests;
  @Autowired InventoryRepository stock;
  @Autowired InventoryCommandService inventories;
  @Autowired IntakeRepository intakes;
  @Autowired MedicationRepository medications;
  @Autowired PlatformTransactionManager transactions;
  @Autowired Clock clock;

  private RequestInventoryConsumptionCommand pending(boolean withStock) {
    var now = clock.instant();
    var adult = UUID.randomUUID().toString();
    var medication = medications.save(Medication.register(adult, "Losartan", "Tablet", now));
    var source =
        Intake.createScheduled(
            UUID.randomUUID().toString(),
            medication.id(),
            adult,
            new MedicationSnapshot("Losartan", "Tablet", "Water"),
            now.plusSeconds(60),
            now);
    source.confirm(now, ConfirmationChannel.TOUCH);
    intakes.saveAll(List.of(source));
    if (withStock)
      inventories.registerInitialInventory(
          new RegisterInitialInventoryCommand(medication.id(), 10, 3));
    var command = new RequestInventoryConsumptionCommand(source.id(), medication.id(), adult, now);
    new TransactionTemplate(transactions).executeWithoutResult(status -> enqueue.handle(command));
    return command;
  }

  private int remaining(RequestInventoryConsumptionCommand command) {
    return stock.findByMedicationId(command.medicationId()).orElseThrow().remainingStock();
  }

  @Test
  void successfulProcessingCompletesIntentAndReplayNeverDecrementsAgain() {
    var command = pending(true);
    process.handle(command);
    process.handle(command);
    assertEquals(9, remaining(command));
    assertTrue(stock.hasConsumed(command.intakeId()));
    assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
    new TransactionTemplate(transactions).executeWithoutResult(status -> enqueue.handle(command));
    assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
  }

  @Test
  void missingInventoryRetainsIntentUntilStockCanBeRegistered() {
    var command = pending(false);
    var original = requests.findByIntakeId(command.intakeId()).orElseThrow();
    assertThrows(RuntimeException.class, () -> process.handle(command));
    assertEquals(original, requests.findByIntakeId(command.intakeId()).orElseThrow());
    assertFalse(stock.hasConsumed(command.intakeId()));
    inventories.registerInitialInventory(
        new RegisterInitialInventoryCommand(command.medicationId(), 10, 3));
    process.handle(command);
    assertEquals(9, remaining(command));
    assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
  }

  @Test
  void completionFailureRollsBackDecrementAndMarkerAndAllowsHealthyRetry() {
    var command = pending(true);
    InventoryConsumptionRequestRepository target = AopTestUtils.getUltimateTargetObject(requests);
    doThrow(new IllegalStateException("controlled completion failure"))
        .when(target)
        .complete(command.intakeId());
    try {
      assertEquals(
          IllegalStateException.class,
          assertThrows(InvalidDataAccessApiUsageException.class, () -> process.handle(command))
              .getCause()
              .getClass());
      assertEquals(10, remaining(command));
      assertFalse(stock.hasConsumed(command.intakeId()));
      assertTrue(requests.findByIntakeId(command.intakeId()).isPresent());
    } finally {
      doCallRealMethod().when(target).complete(command.intakeId());
    }
    process.handle(command);
    assertEquals(9, remaining(command));
    assertTrue(stock.hasConsumed(command.intakeId()));
    assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
  }

  @Test
  void concurrentProcessorsCompleteOneDecrement() throws Exception {
    var command = pending(true);
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                start.await();
                process.handle(command);
                return true;
              });
      var second =
          workers.submit(
              () -> {
                start.await();
                process.handle(command);
                return true;
              });
      start.countDown();
      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    }
    assertEquals(9, remaining(command));
    assertTrue(stock.hasConsumed(command.intakeId()));
    assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
  }

  @Test
  void boundedRecoveryCrossesPagesAndContinuesPastAnUnfulfillableRequest() {
    var unavailable = pending(false);
    var healthy = new java.util.ArrayList<RequestInventoryConsumptionCommand>();
    for (int i = 0; i < 27; i++) healthy.add(pending(true));
    recovery.handle(26);
    assertEquals(1, requests.findByIntakeId(unavailable.intakeId()).orElseThrow().attempts());
    assertFalse(stock.hasConsumed(unavailable.intakeId()));
    assertEquals(25, healthy.stream().filter(item -> stock.hasConsumed(item.intakeId())).count());
    assertEquals(
        2,
        healthy.stream()
            .filter(item -> requests.findByIntakeId(item.intakeId()).isPresent())
            .count());
    recovery.handle(100);
    for (var command : healthy) {
      assertEquals(9, remaining(command));
      assertTrue(stock.hasConsumed(command.intakeId()));
      assertTrue(requests.findByIntakeId(command.intakeId()).isEmpty());
    }
    var stillPending = requests.findByIntakeId(unavailable.intakeId()).orElseThrow();
    assertEquals(1, stillPending.attempts());
    new TransactionTemplate(transactions)
        .executeWithoutResult(status -> requests.complete(unavailable.intakeId()));
  }
}
