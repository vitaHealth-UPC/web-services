package com.vitahealth.tata.inventoryreplenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices.RequestInventoryConsumptionCommandHandler;
import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses actual stored Intake evidence and independent transactions to verify enqueue scope. */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConsumptionEnqueueIntegrationTest {
  @Autowired RequestInventoryConsumptionCommandHandler enqueue;
  @Autowired InventoryConsumptionRequestRepository requests;
  @Autowired IntakeRepository intakes;
  @Autowired PlatformTransactionManager transactions;
  @Autowired Clock clock;

  private Intake intake(boolean confirm) {
    var now = clock.instant();
    var intake =
        Intake.createScheduled(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new MedicationSnapshot("Medication", "Tablet", "Water"),
            now.plusSeconds(60),
            now);
    if (confirm) intake.confirm(now, ConfirmationChannel.TOUCH);
    return intakes.saveAll(List.of(intake)).getFirst();
  }

  private RequestInventoryConsumptionCommand command(Intake intake) {
    return new RequestInventoryConsumptionCommand(
        intake.id(), intake.medicationId(), intake.olderAdultId(), intake.confirmedAt());
  }

  private void write(Runnable action) {
    new TransactionTemplate(transactions).executeWithoutResult(status -> action.run());
  }

  @Test
  void actualConfirmationEnqueuesOnceWithoutRewritingItsRetry() {
    var source = intake(true);
    var command = command(source);
    assertThrows(IllegalTransactionStateException.class, () -> enqueue.handle(command));
    write(() -> enqueue.handle(command));
    var original = requests.findByIntakeId(source.id()).orElseThrow();
    assertTrue(original.matches(command));
    write(() -> requests.recordRetry(original, original.nextAttemptAt().plusSeconds(60)));
    write(() -> enqueue.handle(command));
    assertEquals(
        original.retryAt(original.nextAttemptAt().plusSeconds(60)),
        requests.findByIntakeId(source.id()).orElseThrow());
    write(() -> requests.complete(source.id()));
  }

  @Test
  void wrongOwnerMedicationOrTimeCannotCreateIntent() {
    var source = intake(true);
    var wrong =
        List.of(
            new RequestInventoryConsumptionCommand(
                source.id(), "other", source.olderAdultId(), source.confirmedAt()),
            new RequestInventoryConsumptionCommand(
                source.id(), source.medicationId(), "other", source.confirmedAt()),
            new RequestInventoryConsumptionCommand(
                source.id(),
                source.medicationId(),
                source.olderAdultId(),
                source.confirmedAt().plusNanos(1000)));
    for (var command : wrong)
      assertThrows(IllegalStateException.class, () -> write(() -> enqueue.handle(command)));
    assertTrue(requests.findByIntakeId(source.id()).isEmpty());
  }

  @Test
  void pendingSourceIsRejectedAndRolledBackEnqueueLeavesNoRequest() {
    var pending = intake(false);
    var invalid =
        new RequestInventoryConsumptionCommand(
            pending.id(), pending.medicationId(), pending.olderAdultId(), clock.instant());
    assertThrows(IllegalStateException.class, () -> write(() -> enqueue.handle(invalid)));
    assertTrue(requests.findByIntakeId(pending.id()).isEmpty());
    var source = intake(true);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              enqueue.handle(command(source));
              status.setRollbackOnly();
            });
    assertTrue(requests.findByIntakeId(source.id()).isEmpty());
    assertEquals(source.confirmedAt(), intakes.findById(source.id()).orElseThrow().confirmedAt());
  }

  @Test
  void sourceConfirmationAndIntentShareTheSameRollbackBoundary() {
    var pending = intake(false);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var source = intakes.findByIdForConfirmation(pending.id()).orElseThrow();
              source.confirm(clock.instant(), ConfirmationChannel.TOUCH);
              intakes.saveAll(List.of(source));
              enqueue.handle(command(source));
              assertTrue(requests.findByIntakeId(source.id()).isPresent());
              status.setRollbackOnly();
            });
    assertTrue(intakes.findById(pending.id()).orElseThrow().isPending());
    assertTrue(requests.findByIntakeId(pending.id()).isEmpty());
  }
}
