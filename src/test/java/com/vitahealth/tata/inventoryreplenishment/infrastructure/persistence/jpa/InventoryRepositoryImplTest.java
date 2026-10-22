package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs against H2 without a class-level transaction so each save commits like in production. Every
 * test uses its own medication and intake identifiers.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryRepositoryImplTest {
  private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

  @Autowired InventoryRepository repository;
  @Autowired JdbcTemplate jdbc;
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
        () -> repository.save(Inventory.registerInitial(medicationId, 20, 3, NOW)));
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
        () -> repository.registerConsumption(intakeId, inventory.id(), NOW));
  }

  @Test
  void concurrentUpdateOfSameInventoryIsRejected() {
    var medicationId = newId();
    repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    var outer = new TransactionTemplate(transactionManager);
    var inner = new TransactionTemplate(transactionManager);
    inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThrows(
        OptimisticLockingFailureException.class,
        () ->
            outer.executeWithoutResult(
                status -> {
                  var stale = repository.findByMedicationId(medicationId).orElseThrow();

                  inner.executeWithoutResult(
                      innerStatus -> {
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

  @Test
  void mutationReservationRequiresTheCallerTransaction() {
    assertThrows(
        IllegalTransactionStateException.class,
        () -> repository.findByMedicationIdForUpdate(newId()));
  }

  @Test
  void reservationRefreshesAnInventoryAlreadyLoadedBeforeAnotherCommit() {
    var medicationId = newId();
    repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    var outer = new TransactionTemplate(transactionManager);
    var inner = new TransactionTemplate(transactionManager);
    inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    outer.executeWithoutResult(
        status -> {
          assertEquals(
              10, repository.findByMedicationId(medicationId).orElseThrow().remainingStock());
          inner.executeWithoutResult(
              innerStatus -> {
                var fresh = repository.findByMedicationIdForUpdate(medicationId).orElseThrow();
                fresh.consumeUnit(NOW);
                repository.save(fresh);
              });
          var reserved = repository.findByMedicationIdForUpdate(medicationId).orElseThrow();
          assertEquals(9, reserved.remainingStock());
          reserved.consumeUnit(NOW);
          repository.save(reserved);
        });
    var stored = repository.findByMedicationId(medicationId).orElseThrow();
    assertEquals(8, stored.remainingStock());
    assertEquals(1, stored.batches().size());
  }

  @Test
  void reservationRefreshesReceiptMembershipLoadedBeforeAnotherCommit() {
    var medicationId = newId();
    repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    var outer = new TransactionTemplate(transactionManager);
    var inner = new TransactionTemplate(transactionManager);
    inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    outer.executeWithoutResult(
        status -> {
          assertEquals(
              1, repository.findByMedicationId(medicationId).orElseThrow().batches().size());
          inner.executeWithoutResult(
              innerStatus -> {
                var fresh = repository.findByMedicationIdForUpdate(medicationId).orElseThrow();
                fresh.registerBatch(5, NOW.plusSeconds(60), "Additional lot");
                repository.save(fresh);
              });
          var reserved = repository.findByMedicationIdForUpdate(medicationId).orElseThrow();
          assertEquals(15, reserved.remainingStock());
          assertEquals(2, reserved.batches().size());
          assertEquals("Additional lot", reserved.batches().getLast().lot());
          reserved.consumeUnit(NOW.plusSeconds(60));
          repository.save(reserved);
        });
    var stored = repository.findByMedicationId(medicationId).orElseThrow();
    assertEquals(14, stored.remainingStock());
    assertEquals(2, stored.batches().size());
  }

  @Test
  void competingWriterWaitsUntilTheReservationCommits() throws Exception {
    var medicationId = newId();
    repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    var reserved = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var competitorStarted = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      try {
        var first =
            workers.submit(
                () ->
                    new TransactionTemplate(transactionManager)
                        .execute(
                            status -> {
                              var inventory =
                                  repository
                                      .findByMedicationIdForUpdate(medicationId)
                                      .orElseThrow();
                              reserved.countDown();
                              try {
                                if (!release.await(10, TimeUnit.SECONDS))
                                  throw new AssertionError("reservation was not released");
                              } catch (InterruptedException exception) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(exception);
                              }
                              inventory.consumeUnit(NOW);
                              return repository.save(inventory).remainingStock();
                            }));
        assertTrue(reserved.await(10, TimeUnit.SECONDS));
        var second =
            workers.submit(
                () ->
                    new TransactionTemplate(transactionManager)
                        .execute(
                            status -> {
                              competitorStarted.countDown();
                              var inventory =
                                  repository
                                      .findByMedicationIdForUpdate(medicationId)
                                      .orElseThrow();
                              assertEquals(9, inventory.remainingStock());
                              inventory.consumeUnit(NOW);
                              return repository.save(inventory).remainingStock();
                            }));
        assertTrue(competitorStarted.await(10, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        assertEquals(9, first.get(10, TimeUnit.SECONDS));
        assertEquals(8, second.get(10, TimeUnit.SECONDS));
      } finally {
        release.countDown();
      }
    }
    assertEquals(8, repository.findByMedicationId(medicationId).orElseThrow().remainingStock());
  }

  @Test
  void detachedObsoleteSnapshotCannotOverwriteACommittedDecrement() {
    var medicationId = newId();
    var stored = repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    var obsolete = repository.findByMedicationId(medicationId).orElseThrow();
    var current = repository.findByMedicationId(medicationId).orElseThrow();
    current.consumeUnit(NOW);
    var winner = repository.save(current);
    assertTrue(winner.revision() > stored.revision());
    obsolete.registerBatch(5, NOW.plusSeconds(60));
    assertThrows(OptimisticLockingFailureException.class, () -> repository.save(obsolete));
    var retained = repository.findByMedicationId(medicationId).orElseThrow();
    assertEquals(9, retained.remainingStock());
    assertEquals(1, retained.batches().size());
    assertEquals(winner.revision(), retained.revision());
  }

  @Test
  void currentRevisionCannotRewriteOrDropReceiptsOrInventStock() {
    var medicationId = newId();
    var created = repository.save(Inventory.registerInitial(medicationId, 10, 3, NOW));
    created.registerBatch(20, NOW.plusSeconds(60), "Original lot");
    for (int i = 0; i < 25; i++) created.consumeUnit(NOW.plusSeconds(120));
    var stored = repository.save(created);
    var parentBefore = jdbc.queryForMap("select * from inventories where id = ?", stored.id());
    var receiptsBefore =
        jdbc.queryForList(
            "select * from inventory_batches where inventory_id = ? order by id", stored.id());
    var first = stored.batches().getFirst();
    var second = stored.batches().getLast();
    var histories =
        List.of(
            List.of(first),
            List.of(
                Batch.rehydrate(
                    first.id(), first.quantity() + 1, first.registeredAt(), first.lot()),
                second),
            List.of(
                first,
                Batch.rehydrate(
                    second.id(), second.quantity(), second.registeredAt(), "Changed lot")),
            List.of(
                Batch.rehydrate(
                    first.id(), first.quantity(), first.registeredAt().plusSeconds(1), first.lot()),
                second));
    var candidates = new ArrayList<Inventory>();
    for (var history : histories)
      candidates.add(
          Inventory.rehydrate(
              stored.id(),
              medicationId,
              stored.remainingStock(),
              stored.replenishmentThreshold(),
              history,
              NOW,
              stored.updatedAt(),
              stored.revision()));
    candidates.add(
        Inventory.rehydrate(
            stored.id(),
            medicationId,
            stored.remainingStock() + 1,
            stored.replenishmentThreshold(),
            stored.batches(),
            NOW,
            stored.updatedAt(),
            stored.revision()));
    candidates.add(
        Inventory.rehydrate(
            stored.id(),
            medicationId,
            stored.remainingStock(),
            stored.replenishmentThreshold(),
            stored.batches(),
            NOW,
            NOW.plusSeconds(90),
            stored.revision()));
    for (var candidate : candidates) {
      var failure =
          assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.save(candidate));
      assertEquals(IllegalStateException.class, failure.getCause().getClass());
      assertEquals(
          parentBefore, jdbc.queryForMap("select * from inventories where id = ?", stored.id()));
      assertEquals(
          receiptsBefore,
          jdbc.queryForList(
              "select * from inventory_batches where inventory_id = ? order by id", stored.id()));
    }
    stored.registerBatch(2, NOW.plusSeconds(180), "Healthy lot");
    var healthy = repository.save(stored);
    assertEquals(7, healthy.remainingStock());
    assertEquals(3, healthy.batches().size());
  }
}
