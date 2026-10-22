package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies source ordering independently from event arrival time and shortage visibility. */
class InventoryStockPositionTest {
  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

  private InventoryStockPosition position(long revision, int remaining) {
    return new InventoryStockPosition("medication", "inventory", revision, remaining, 3, NOW);
  }

  @Test
  void newerHealthyEvidencePreventsOlderShortageFromAdvancing() {
    var low = position(1, 2);
    var healthy = position(2, 10);
    assertTrue(healthy.advances(low));
    assertFalse(low.advances(healthy));
    assertTrue(low.isLowStock());
    assertFalse(healthy.isLowStock());
    assertTrue(position(3, 3).isLowStock());
  }

  @Test
  void equalTimestampAllowsRevisionProgressAndExactReplayDoesNothing() {
    assertTrue(position(2, 1).advances(position(1, 2)));
    assertFalse(position(2, 1).advances(position(2, 1)));
  }

  @Test
  void sameRevisionRejectsEveryConflictingPayloadField() {
    var original = position(1, 2);
    for (var conflicting :
        List.of(
            position(1, 1),
            new InventoryStockPosition("medication", "inventory", 1, 2, 4, NOW),
            new InventoryStockPosition("medication", "inventory", 1, 2, 3, NOW.plusSeconds(1)))) {
      assertThrows(IllegalStateException.class, () -> conflicting.advances(original));
    }
  }

  @Test
  void unrelatedSourceIdentityCannotReplaceAnExistingPositionEvenWithOlderRevision() {
    var previous = position(9, 10);
    assertThrows(
        IllegalStateException.class,
        () -> new InventoryStockPosition("other", "inventory", 1, 2, 3, NOW).advances(previous));
    assertThrows(
        IllegalStateException.class,
        () -> new InventoryStockPosition("medication", "other", 1, 2, 3, NOW).advances(previous));
  }

  @Test
  void rejectsMissingIdentityTimeAndNegativeEvidence() {
    assertThrows(IllegalArgumentException.class, () -> position(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> position(1, -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InventoryStockPosition("medication", "inventory", 1, 1, -1, NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InventoryStockPosition(" ", "inventory", 1, 1, 3, NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InventoryStockPosition("medication", " ", 1, 1, 3, NOW));
    assertThrows(
        NullPointerException.class,
        () -> new InventoryStockPosition("medication", "inventory", 1, 1, 3, null));
  }
}
