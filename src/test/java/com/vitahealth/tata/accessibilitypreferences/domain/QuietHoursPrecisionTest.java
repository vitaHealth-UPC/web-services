package com.vitahealth.tata.accessibilitypreferences.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** Verifies silence boundaries without silently rounding user-supplied times. */
class QuietHoursPrecisionTest {
  @Test
  void rejectsSecondsAndFractionsOnEitherBoundary() {
    var start = LocalTime.of(22, 0);
    var end = LocalTime.of(7, 0);
    assertThrows(
        IllegalArgumentException.class, () -> QuietHoursRange.between(start.plusSeconds(1), end));
    assertThrows(
        IllegalArgumentException.class, () -> QuietHoursRange.between(start, end.plusNanos(1)));
    assertThrows(IllegalArgumentException.class, () -> QuietHoursRange.between(null, end));
  }

  @Test
  void overnightIntervalRetainsInclusiveStartAndExclusiveEnd() {
    var range = QuietHoursRange.between(LocalTime.of(22, 0), LocalTime.of(7, 0));
    assertTrue(range.contains(22, 0));
    assertTrue(range.contains(0, 0));
    assertTrue(range.contains(6, 59));
    assertFalse(range.contains(7, 0));
    assertFalse(range.contains(21, 59));
    assertThrows(IllegalArgumentException.class, () -> range.contains(24, 0));
    assertThrows(IllegalArgumentException.class, () -> range.contains(0, 60));
  }
}
