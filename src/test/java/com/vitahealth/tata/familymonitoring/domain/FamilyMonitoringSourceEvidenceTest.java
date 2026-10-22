package com.vitahealth.tata.familymonitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Verifies source evidence validity before refreshing stock or restoring analytics history. */
class FamilyMonitoringSourceEvidenceTest {
  private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");
  private static final LocalDate FIRST = LocalDate.of(2026, 10, 12);

  @Test
  void invalidAndOlderStockRefreshesLeaveEveryOriginalFieldUntouched() {
    var notice = LowStockNotice.rehydrate(3L, "medication", "Original", 2, 5, NOW);
    assertThrows(
        IllegalArgumentException.class, () -> notice.refresh("New", 1, 5, NOW.minusNanos(1)));
    assertThrows(
        IllegalArgumentException.class, () -> notice.refresh("New", -1, 5, NOW.plusSeconds(1)));
    assertThrows(
        IllegalArgumentException.class, () -> notice.refresh("New", 6, 5, NOW.plusSeconds(1)));
    assertThrows(
        IllegalArgumentException.class, () -> notice.refresh(" ", 1, 5, NOW.plusSeconds(1)));
    assertThrows(NullPointerException.class, () -> notice.refresh("New", 1, 5, null));
    assertEquals("Original", notice.getMedicationName());
    assertEquals(2, notice.getRemainingStock());
    assertEquals(5, notice.getReplenishmentThreshold());
    assertEquals(NOW, notice.getDetectedAt());
    notice.refresh("New", 0, 4, NOW.plusSeconds(1));
    assertEquals(3L, notice.getId());
    assertEquals("medication", notice.getMedicationId());
    assertEquals(0, notice.getRemainingStock());
    assertEquals(4, notice.getReplenishmentThreshold());
    assertEquals(NOW.plusSeconds(1), notice.getDetectedAt());
  }

  @Test
  void stockCreationAndRestorationRejectInvalidSourceQuantities() {
    for (int[] quantities : new int[][] {{-1, 5}, {0, -1}, {6, 5}}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new LowStockNotice("medication", "Name", quantities[0], quantities[1], NOW));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              LowStockNotice.rehydrate(
                  1L, "medication", "Name", quantities[0], quantities[1], NOW));
    }
    var zero = new LowStockNotice("medication", "Name", 0, 0, NOW);
    assertEquals(0, zero.getRemainingStock());
  }

  @Test
  void analyticsDayCountsMustFitTheInclusiveSourcePeriodOnBothPaths() {
    for (int count : new int[] {-1, 0, 8}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new AdherenceInsight("medication", "Name", count, FIRST, FIRST.plusDays(6), NOW));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              AdherenceInsight.rehydrate(
                  1L, "medication", "Name", count, FIRST, FIRST.plusDays(6), NOW));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdherenceInsight("medication", "Name", 1, FIRST, FIRST.minusDays(1), NOW));
    var restored =
        AdherenceInsight.rehydrate(
            8L, "medication", " Original name ", 7, FIRST, FIRST.plusDays(6), NOW);
    assertEquals(7, restored.getOmissionDays());
    assertEquals(" Original name ", restored.getMedicationName());
    assertEquals(FIRST, restored.getFirstDay());
    assertEquals(FIRST.plusDays(6), restored.getLastDay());
    assertEquals(NOW, restored.getDetectedAt());
  }

  @Test
  void restorationRequiresStoredIdentityAndCompleteSourceEvidence() {
    for (Long id : new Long[] {null, 0L, -1L}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> LowStockNotice.rehydrate(id, "medication", "Name", 1, 5, NOW));
      assertThrows(
          IllegalArgumentException.class,
          () -> AdherenceInsight.rehydrate(id, "medication", "Name", 1, FIRST, FIRST, NOW));
    }
    assertThrows(IllegalArgumentException.class, () -> new LowStockNotice(" ", "Name", 1, 5, NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdherenceInsight("medication", " ", 1, FIRST, FIRST, NOW));
    assertThrows(
        NullPointerException.class,
        () -> new AdherenceInsight("medication", "Name", 1, FIRST, null, NOW));
    assertThrows(
        NullPointerException.class,
        () -> new AdherenceInsight("medication", "Name", 1, FIRST, FIRST, null));
  }
}
