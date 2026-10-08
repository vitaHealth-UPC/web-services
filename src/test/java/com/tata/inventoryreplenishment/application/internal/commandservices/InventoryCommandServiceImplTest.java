package com.tata.inventoryreplenishment.application.internal.commandservices;

import com.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.tata.inventoryreplenishment.application.internal.fakes.InMemoryInventoryRepository;
import com.tata.inventoryreplenishment.application.internal.fakes.RecordingInventoryEventPublisher;
import com.tata.inventoryreplenishment.domain.model.commands.ConsumeUnitCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterInitialInventoryCommand;
import com.tata.inventoryreplenishment.domain.model.commands.RegisterReplenishmentCommand;
import com.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import com.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class InventoryCommandServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

    private InMemoryInventoryRepository repository;
    private RecordingInventoryEventPublisher publisher;
    private InventoryCommandServiceImpl service;

    @Test
    void rejectsUnknownMedicationWithoutPersistingStockOrPublishingEvents() {
        var guarded = new InventoryCommandServiceImpl(repository, publisher, id -> com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog.Availability.MISSING,
                Clock.fixed(NOW, ZoneOffset.UTC));
        var error = assertThrows(InventoryApplicationException.class, () -> guarded.registerInitialInventory(
                new RegisterInitialInventoryCommand("missing", 10, 2)));
        assertEquals(InventoryApplicationException.Code.MEDICATION_NOT_FOUND, error.code());
        assertTrue(repository.findByMedicationId("missing").isEmpty());
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryInventoryRepository();
        publisher = new RecordingInventoryEventPublisher();
        service = new InventoryCommandServiceImpl(repository, publisher, id -> com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog.Availability.ACTIVE, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registersInitialInventory() {
        var result = service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 30, 5));

        assertEquals("medication-1", result.medicationId());
        assertEquals(30, result.remainingStock());
        assertEquals(5, result.replenishmentThreshold());
        assertFalse(result.lowStock());
        assertEquals(1, result.batches().size());
        assertEquals(NOW, result.createdAt());
        assertTrue(publisher.events.isEmpty());
    }

    @Test
    void rejectsSecondInventoryForSameMedication() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 30, 5));

        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, 2))
        );

        assertEquals(InventoryApplicationException.Code.INVENTORY_ALREADY_EXISTS, exception.code());
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void rejectsInvalidInitialQuantityAndThreshold() {
        var zeroQuantity = assertThrows(
                InventoryApplicationException.class,
                () -> service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 0, 5))
        );
        var negativeThreshold = assertThrows(
                InventoryApplicationException.class,
                () -> service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, -1))
        );

        assertEquals(InventoryApplicationException.Code.INVALID_QUANTITY, zeroQuantity.code());
        assertEquals(InventoryApplicationException.Code.INVALID_QUANTITY, negativeThreshold.code());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void replenishmentIncreasesStockAndPublishesEvent() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, 5));

        var result = service.registerReplenishment(new RegisterReplenishmentCommand("medication-1", 20));

        assertEquals(30, result.remainingStock());
        assertEquals(2, result.batches().size());
        assertEquals(1, publisher.events.size());
        var event = assertInstanceOf(ReplenishmentRegistered.class, publisher.events.getFirst());
        assertEquals("medication-1", event.medicationId());
        assertEquals(20, event.quantity());
    }

    @Test
    void rejectsInvalidReplenishmentQuantity() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, 5));

        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.registerReplenishment(new RegisterReplenishmentCommand("medication-1", 0))
        );

        assertEquals(InventoryApplicationException.Code.INVALID_QUANTITY, exception.code());
        assertTrue(publisher.events.isEmpty());
    }

    @Test
    void replenishmentRequiresExistingInventory() {
        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.registerReplenishment(new RegisterReplenishmentCommand("missing", 10))
        );

        assertEquals(InventoryApplicationException.Code.INVENTORY_NOT_FOUND, exception.code());
    }

    @Test
    void confirmedIntakeDecrementsStockOnlyOnce() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, 2));

        assertTrue(service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-1")));
        assertFalse(service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-1")));

        assertEquals(9, repository.findByMedicationId("medication-1").orElseThrow().remainingStock());
        assertEquals(1, repository.consumptionCount());
    }

    @Test
    void differentIntakesEachConsumeOneUnit() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 10, 2));

        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-1"));
        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-2"));

        assertEquals(8, repository.findByMedicationId("medication-1").orElseThrow().remainingStock());
        assertEquals(2, repository.consumptionCount());
    }

    @Test
    void publishesLowStockDetectedOnceWhenCrossingThreshold() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 6, 5));

        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-1"));
        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-2"));
        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-3"));

        var lowStockEvents = publisher.events.stream().filter(LowStockDetected.class::isInstance).toList();
        assertEquals(1, lowStockEvents.size());
        assertEquals(5, ((LowStockDetected) lowStockEvents.getFirst()).remainingStock());
    }

    @Test
    void consumingWithoutStockFailsAndDoesNotRegisterConsumption() {
        service.registerInitialInventory(new RegisterInitialInventoryCommand("medication-1", 1, 0));
        service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-1"));

        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.consumeUnit(new ConsumeUnitCommand("medication-1", "intake-2"))
        );

        assertEquals(InventoryApplicationException.Code.INSUFFICIENT_STOCK, exception.code());
        assertEquals(0, repository.findByMedicationId("medication-1").orElseThrow().remainingStock());
        assertFalse(repository.hasConsumed("intake-2"));
    }

    @Test
    void consumingRequiresExistingInventory() {
        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.consumeUnit(new ConsumeUnitCommand("missing", "intake-1"))
        );

        assertEquals(InventoryApplicationException.Code.INVENTORY_NOT_FOUND, exception.code());
        assertEquals(0, repository.consumptionCount());
    }
}
