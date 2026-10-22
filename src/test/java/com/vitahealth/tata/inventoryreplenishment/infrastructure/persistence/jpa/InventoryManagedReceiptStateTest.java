package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.BatchPersistenceEntity;
import com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa.entities.InventoryPersistenceEntity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Confirms an invalid candidate cannot partially mutate managed parent or receipt state. */
class InventoryManagedReceiptStateTest {
  @Test
  void invalidCandidateLeavesManagedQuantityTimeAndReceiptInstancesUntouched() {
    var now = Instant.parse("2026-10-20T00:00:00Z");
    var parent = new InventoryPersistenceEntity("inventory", "medicine", 10, 3, now, now);
    var receipt = new BatchPersistenceEntity("receipt", 10, now, "Original");
    parent.updateStock(10, List.of(receipt), now);
    var added = new BatchPersistenceEntity("new", 2, now.plusSeconds(60), "New");
    var altered = new BatchPersistenceEntity("receipt", 11, now, "Original");
    assertThrows(
        IllegalStateException.class,
        () -> parent.updateStock(12, List.of(added, altered), now.plusSeconds(60)));
    assertEquals(10, parent.getRemainingStock());
    assertEquals(now, parent.getUpdatedAt());
    assertEquals(1, parent.getBatches().size());
    assertSame(receipt, parent.getBatches().getFirst());
    assertThrows(UnsupportedOperationException.class, () -> parent.getBatches().clear());
    parent.updateStock(12, List.of(receipt, added), now.plusSeconds(60));
    assertEquals(12, parent.getRemainingStock());
    assertSame(receipt, parent.getBatches().getFirst());
    assertSame(added, parent.getBatches().getLast());
  }
}
