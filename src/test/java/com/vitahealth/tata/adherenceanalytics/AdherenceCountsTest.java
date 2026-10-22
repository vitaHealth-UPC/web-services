package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceCounts;
import org.junit.jupiter.api.Test;

/** Checks shared denominator, precision and overflow rules for definitive adherence evidence. */
class AdherenceCountsTest {
  @Test
  void retainsExactAndRoundedPercentagesAndEmptyEvidence() {
    var counts = new AdherenceCounts(1, 1, 1);
    assertEquals(2, counts.confirmed());
    assertEquals(3, counts.total());
    assertEquals(200d / 3, counts.percentage());
    assertEquals(67, counts.roundedAdherencePercent());
    assertEquals(33, counts.roundedOnTimePercent());
    assertEquals(0d, new AdherenceCounts(0, 0, 0).percentage());
  }

  @Test
  void rejectsNegativeAndOverflowingCounts() {
    assertThrows(IllegalArgumentException.class, () -> new AdherenceCounts(-1, 0, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new AdherenceCounts(Integer.MAX_VALUE, 1, 0));
    assertEquals(Integer.MAX_VALUE, new AdherenceCounts(Integer.MAX_VALUE, 0, 0).total());
  }
}
