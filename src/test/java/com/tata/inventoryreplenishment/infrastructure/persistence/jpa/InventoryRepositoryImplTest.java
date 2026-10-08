package com.tata.inventoryreplenishment.infrastructure.persistence.jpa;

import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against H2 without a class-level transaction so each save commits like in production.
 * Every test uses its own medication and intake identifiers.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryRepositoryImplTest {
    private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

    @Autowired InventoryRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void savesAndReloadsInventoryWithBatchesInOrder() {
        var medicationId = newId();
        var inventory = Inventory.registerInitial(medicationId, 10, 3, NOW);
        repository.save(inventory);

        var loaded = repository.findByMedicationId(medicationId).orElseThrow();
        loaded.registerBatch(5, NOW.plusSeconds(60), " LOTE-2026-09 ");
        repository.save(loaded);

        var reloaded = repository.findByMedicationId(medicationId).orElseThrow();
        assertEquals(inventory.id(), reloaded.id());
        assertEquals(15, reloaded.remainingStock());
        assertEquals(3, reloaded.replenishmentThreshold());
        assertEquals(2, reloaded.batches().size());
        assertEquals(10, reloaded.batches().get(0).quantity());
        assertEquals(5, reloaded.batches().get(1).quantity());
        assertEquals("LOTE-2026-09", reloaded.batches().get(1).lot());
        assertTrue(repository.existsByMedicationId(medicationId));
    }

    @Test
    void savingAgainDoesNotDuplicateBatches() {
        var medicationId = newId();
        repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));

        var loaded = repository.findByMedicationId(medicationId).orElseThrow();
        loaded.consumeUnit(NOW);
        repository.save(loaded);

        var reloaded = repository.findByMedicationId(medicationId).orElseThrow();
        assertEquals(9, reloaded.remainingStock());
        assertEquals(1, reloaded.batches().size());
    }

    @Test
    void returnsEmptyForUnknownMedication() {
        assertTrue(repository.findByMedicationId(newId()).isEmpty());
        assertFalse(repository.existsByMedicationId(newId()));
    }

    @Test
    void rejectsSecondInventoryForSameMedication() {
        var medicationId = newId();
        repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(Inventory.registerInitial(medicationId, 20, 3, NOW))
        );
    }

    @Test
    void tracksConsumptionOncePerIntake() {
        var inventory = repository.save(Inventory.registerInitial(newId(), 10, 3, NOW));
        var intakeId = newId();

        assertFalse(repository.hasConsumed(intakeId));
        repository.registerConsumption(intakeId, inventory.id(), NOW);
        assertTrue(repository.hasConsumed(intakeId));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.registerConsumption(intakeId, inventory.id(), NOW)
        );
    }

    @Test
    void concurrentUpdateOfSameInventoryIsRejected() {
        var medicationId = newId();
        repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
        var outer = new TransactionTemplate(transactionManager);
        var inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(OptimisticLockingFailureException.class, () -> outer.executeWithoutResult(status -> {
            var stale = repository.findByMedicationId(medicationId).orElseThrow();

            inner.executeWithoutResult(innerStatus -> {
                var fresh = repository.findByMedicationId(medicationId).orElseThrow();
                fresh.consumeUnit(NOW);
                repository.save(fresh);
            });

            stale.consumeUnit(NOW);
            repository.save(stale);
        }));

        assertEquals(9, repository.findByMedicationId(medicationId).orElseThrow().remainingStock());
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
