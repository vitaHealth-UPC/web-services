package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies resumable progress and retry chronology without infrastructure dependencies. */
class FamilyStockCatchUpTest {
  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

  @Test
  void advancesWithoutLosingOriginalRequestAndRetryRetainsProgress() {
    var first = FamilyStockCatchUp.start(1, NOW).advance("medication-1", NOW);
    var retry = first.retryAt(NOW.plusSeconds(30));
    assertEquals("medication-1", retry.afterMedicationId());
    assertEquals(NOW, retry.requestedAt());
    assertEquals(1, retry.attempts());
    var next = retry.advance("medication-2", NOW.plusSeconds(30));
    assertEquals(0, next.attempts());
    assertEquals(NOW, next.requestedAt());
    assertThrows(IllegalArgumentException.class, () -> retry.advance("medication-2", NOW));
    assertThrows(IllegalArgumentException.class, () -> first.advance("medication-1", NOW));
    assertThrows(IllegalArgumentException.class, () -> first.advance("", NOW));
  }

  @Test
  void rejectsMalformedOrBackwardRecoveryEvidence() {
    assertThrows(IllegalArgumentException.class, () -> FamilyStockCatchUp.start(0, NOW));
    assertThrows(NullPointerException.class, () -> FamilyStockCatchUp.start(1, null));
    assertThrows(IllegalArgumentException.class, () -> new FamilyStockCatchUp(1, " ", NOW, NOW, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FamilyStockCatchUp(1, "", NOW, NOW.minusSeconds(1), 0));
    assertThrows(IllegalArgumentException.class, () -> new FamilyStockCatchUp(1, "", NOW, NOW, -1));
    assertThrows(
        ArithmeticException.class,
        () -> new FamilyStockCatchUp(1, "", NOW, NOW, Integer.MAX_VALUE).retryAt(NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> FamilyStockCatchUp.start(1, NOW).retryAt(NOW.minusSeconds(1)));
  }
}
