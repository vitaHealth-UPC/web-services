package com.tata.inventoryreplenishment;

import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.intakeexecution.domain.model.valueobjects.*;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IntakeInventoryIntegrationTest {
    @Autowired ConfirmIntakeCommandHandler confirmations;
    @Autowired IntakeRepository intakes;
    @Autowired com.tata.treatmentmanagement.domain.repositories.MedicationRepository medications;
    @Autowired InventoryCommandService commands;
    @Autowired InventoryRepository inventories;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;

    private Intake intake() {
        var now = Instant.now();
        var adultId = UUID.randomUUID().toString();
        var medication = medications.save(com.tata.treatmentmanagement.domain.model.aggregates.Medication.register(
                adultId, "Losartan", "Tablet", now));
        var intake = Intake.createScheduled(
            UUID.randomUUID().toString(),
            medication.id(),
            adultId,
            new MedicationSnapshot("Losartan", "1 tablet", "With water"),
            now.plusSeconds(60),
            now
        );
        return intakes.saveAll(List.of(intake)).getFirst();
    }
    private void publish(IntakeConfirmed event) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> events.publishEvent(event));
    }
    @Test void committedConfirmationAndRetriesConsumeOneUnit() {
        var intake = intake();
        commands.registerInitialInventory(new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
        confirmations.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH));
        confirmations.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.VOICE));
        publish(new IntakeConfirmed(intake.id(), intake.medicationId(), intake.olderAdultId(), Instant.now()));
        assertEquals(9, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
        assertTrue(inventories.hasConsumed(intake.id()));
    }
    @Test void rolledBackConfirmationEventDoesNotConsumeStock() {
        var intake = intake();
        commands.registerInitialInventory(new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(new IntakeConfirmed(intake.id(), intake.medicationId(), intake.olderAdultId(), Instant.now()));
            status.setRollbackOnly();
        });
        assertEquals(10, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
        assertFalse(inventories.hasConsumed(intake.id()));
    }
    @Test void simultaneousRedeliveryConsumesOneUnit() throws Exception {
        var intake = intake();
        commands.registerInitialInventory(new RegisterInitialInventoryCommand(intake.medicationId(), 10, 3));
        var event = new IntakeConfirmed(intake.id(), intake.medicationId(), intake.olderAdultId(), Instant.now());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); publish(event); return true; });
            var second = executor.submit(() -> { start.await(); publish(event); return true; });
            start.countDown();
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(9, inventories.findByMedicationId(intake.medicationId()).orElseThrow().remainingStock());
        assertTrue(inventories.hasConsumed(intake.id()));
    }
    @Test void missingOrEmptyInventoryDoesNotUndoConfirmation() {
        var missing = intake();
        confirmations.handle(new ConfirmIntakeCommand(missing.id(), ConfirmationChannel.TOUCH));
        assertEquals(IntakeStatus.CONFIRMED, intakes.findById(missing.id()).orElseThrow().status());
        var empty = intake();
        commands.registerInitialInventory(new RegisterInitialInventoryCommand(empty.medicationId(), 1, 3));
        commands.consumeUnit(new com.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand(empty.medicationId(), UUID.randomUUID().toString()));
        confirmations.handle(new ConfirmIntakeCommand(empty.id(), ConfirmationChannel.TOUCH));
        assertEquals(IntakeStatus.CONFIRMED, intakes.findById(empty.id()).orElseThrow().status());
        assertEquals(0, inventories.findByMedicationId(empty.medicationId()).orElseThrow().remainingStock());
        assertFalse(inventories.hasConsumed(empty.id()));
    }
}