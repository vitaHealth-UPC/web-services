package com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.inventoryreplenishment.domain.model.entities.Batch;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.LowStockDetected;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.ReplenishmentRegistered;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryTest {
  private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

  @Test
  void registersInitialStockAsFirstBatchWithoutEvents() {
    var inventory = Inventory.registerInitial(" medication-1 ", 30, 5, NOW);

    assertEquals("medication-1", inventory.medicationId());
    assertEquals(30, inventory.remainingStock());
    assertEquals(1, inventory.batches().size());
    assertEquals(30, inventory.batches().getFirst().quantity());
    assertFalse(inventory.isLowStock());
    assertTrue(inventory.pullDomainEvents().isEmpty());
  }

  @Test
  void rejectsNonPositiveInitialQuantity() {
    assertThrows(
        IllegalArgumentException.class, () -> Inventory.registerInitial("medication-1", 0, 5, NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> Inventory.registerInitial("medication-1", -3, 5, NOW));
  }

  @Test
  void rejectsNegativeThreshold() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Inventory.registerInitial("medication-1", 10, -1, NOW));
  }

  @Test
  void rejectsMissingMedication() {
    assertThrows(IllegalArgumentException.class, () -> Inventory.registerInitial(" ", 10, 2, NOW));
  }

  @Test
  void replenishmentAddsBatchAndRecordsEvent() {
    var inventory = Inventory.registerInitial("medication-1", 10, 5, NOW);
    var later = NOW.plusSeconds(3600);

    var batch = inventory.registerBatch(20, later);

    assertEquals(30, inventory.remainingStock());
    assertEquals(2, inventory.batches().size());
    assertEquals(later, inventory.updatedAt());
    var events = inventory.pullDomainEvents();
    assertEquals(1, events.size());
    var event = assertInstanceOf(ReplenishmentRegistered.class, events.getFirst());
    assertEquals(batch.id(), event.batchId());
    assertEquals(20, event.quantity());
    assertEquals(30, event.remainingStock());
  }

  @Test
  void rejectsNonPositiveReplenishment() {
    var inventory = Inventory.registerInitial("medication-1", 10, 5, NOW);

    assertThrows(IllegalArgumentException.class, () -> inventory.registerBatch(0, NOW));
    assertEquals(10, inventory.remainingStock());
    assertEquals(1, inventory.batches().size());
  }

  @Test
  void rejectsReplenishmentThatOverflowsStock() {
    var inventory = Inventory.registerInitial("medication-1", Integer.MAX_VALUE, 5, NOW);

    assertThrows(ArithmeticException.class, () -> inventory.registerBatch(1, NOW));
    assertEquals(Integer.MAX_VALUE, inventory.remainingStock());
    assertEquals(1, inventory.batches().size());
  }

  @Test
  void consumeUnitDecrementsStock() {
    var inventory = Inventory.registerInitial("medication-1", 10, 2, NOW);

    inventory.consumeUnit(NOW);

    assertEquals(9, inventory.remainingStock());
  }

  @Test
  void stockCannotBecomeNegative() {
    var inventory = Inventory.registerInitial("medication-1", 1, 0, NOW);
    inventory.consumeUnit(NOW);

    assertThrows(IllegalStateException.class, () -> inventory.consumeUnit(NOW));
    assertEquals(0, inventory.remainingStock());
  }

  @Test
  void lowStockIsDetectedOnlyWhenCrossingThreshold() {
    var inventory = Inventory.registerInitial("medication-1", 7, 5, NOW);

    inventory.consumeUnit(NOW);
    assertTrue(inventory.pullDomainEvents().isEmpty());

    inventory.consumeUnit(NOW);
    var crossing = inventory.pullDomainEvents();
    assertEquals(1, crossing.size());
    var event = assertInstanceOf(LowStockDetected.class, crossing.getFirst());
    assertEquals(5, event.remainingStock());
    assertEquals(5, event.replenishmentThreshold());

    inventory.consumeUnit(NOW);
    inventory.consumeUnit(NOW);
    assertTrue(inventory.pullDomainEvents().isEmpty());
  }

  @Test
  void lowStockIsDetectedAgainAfterReplenishmentAboveThreshold() {
    var inventory = Inventory.registerInitial("medication-1", 6, 5, NOW);
    inventory.consumeUnit(NOW);
    inventory.registerBatch(10, NOW);
    assertFalse(inventory.isLowStock());
    inventory.pullDomainEvents();

    for (int i = 0; i < 10; i++) {
      inventory.consumeUnit(NOW);
    }

    var lowStockEvents =
        inventory.pullDomainEvents().stream().filter(LowStockDetected.class::isInstance).toList();
    assertEquals(1, lowStockEvents.size());
  }

  @Test
  void initialStockAtThresholdIsLowButDoesNotRecordCrossing() {
    var inventory = Inventory.registerInitial("medication-1", 5, 5, NOW);

    assertTrue(inventory.isLowStock());
    assertTrue(inventory.pullDomainEvents().isEmpty());
  }

  @Test
  void pullDomainEventsClearsRecordedEvents() {
    var inventory = Inventory.registerInitial("medication-1", 10, 5, NOW);
    inventory.registerBatch(5, NOW);

    assertEquals(1, inventory.pullDomainEvents().size());
    assertTrue(inventory.pullDomainEvents().isEmpty());
  }

  @Test
  void rehydrateRejectsNegativeStock() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Inventory.rehydrate(
                "inventory-1",
                "medication-1",
                -1,
                5,
                List.of(Batch.rehydrate("batch-1", 10, NOW)),
                NOW,
                NOW,
                0L));
  }

  @Test
  void restorationRejectsImpossibleAuditAndReceiptHistory() {
    var receipt = Batch.rehydrate("batch-1", 10, NOW);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Inventory.rehydrate(
                "inventory-1",
                "medication-1",
                5,
                2,
                List.of(receipt),
                NOW,
                NOW.minusSeconds(1),
                0L));
    assertThrows(
        IllegalArgumentException.class,
        () -> Inventory.rehydrate("inventory-1", "medication-1", 0, 2, List.of(), NOW, NOW, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Inventory.rehydrate(
                "inventory-1", "medication-1", 5, 2, List.of(receipt, receipt), NOW, NOW, 0L));
    assertThrows(
        NullPointerException.class,
        () ->
            Inventory.rehydrate(
                "inventory-1", "medication-1", 5, 2, Arrays.asList(receipt, null), NOW, NOW, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Inventory.rehydrate(
                "inventory-1", "medication-1", 11, 2, List.of(receipt), NOW, NOW, 0L));
    for (Instant outside : List.of(NOW.minusSeconds(1), NOW.plusSeconds(2))) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Inventory.rehydrate(
                  "inventory-1",
                  "medication-1",
                  5,
                  2,
                  List.of(Batch.rehydrate("batch-1", 10, outside)),
                  NOW,
                  NOW.plusSeconds(1),
                  0L));
    }
  }

  @Test
  void restorationPreservesHistoricalTextAndReceiptsWithoutEmittingEvents() {
    var later = NOW.plusSeconds(60);
    var first = Batch.rehydrate("batch-1", Integer.MAX_VALUE, NOW, "  Original lot  ");
    var second = Batch.rehydrate("batch-2", Integer.MAX_VALUE, later);
    var source = new ArrayList<>(List.of(second, first));
    var restored = Inventory.rehydrate("inventory-1", "medication-1", 3, 5, source, NOW, later, 0L);
    source.clear();
    assertEquals(List.of(second, first), restored.batches());
    assertEquals("  Original lot  ", restored.batches().get(1).lot());
    assertEquals(3, restored.remainingStock());
    assertEquals(later, restored.updatedAt());
    assertTrue(restored.pullDomainEvents().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> restored.batches().clear());
    assertEquals("New lot", Batch.register(1, later, "  New lot  ").lot());
  }

  @Test
  void invalidMutationTimeCannotChangeStockReceiptsOrEvents() {
    var inventory = Inventory.registerInitial("medication-1", 6, 5, NOW);
    var before = inventory.batches();
    for (Instant invalid : Arrays.asList(null, NOW.minusNanos(1))) {
      assertThrows(RuntimeException.class, () -> inventory.registerBatch(2, invalid, "Lot"));
      assertThrows(RuntimeException.class, () -> inventory.consumeUnit(invalid));
      assertEquals(6, inventory.remainingStock());
      assertEquals(before, inventory.batches());
      assertEquals(NOW, inventory.updatedAt());
      assertTrue(inventory.pullDomainEvents().isEmpty());
    }
    inventory.consumeUnit(NOW);
    assertEquals(5, inventory.remainingStock());
    assertEquals(1, inventory.pullDomainEvents().size());
    inventory.registerBatch(1, NOW);
    assertEquals(6, inventory.remainingStock());
    assertEquals(1, inventory.pullDomainEvents().size());
  }
}
