package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceHourRange;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies that midnight does not expand a recurring night pattern into a full-day interval. */
class AdherenceHourRangeTest {
  @Test
  void nightHoursUseCircularClockOrdering() {
    assertEquals(
        new AdherenceHourRange(22, 3),
        AdherenceHourRange.ofHours(List.of(22, 1, 2), TimeBand.NIGHT));
    assertEquals(
        new AdherenceHourRange(0, 3), AdherenceHourRange.ofHours(List.of(0, 2), TimeBand.NIGHT));
    assertEquals(
        new AdherenceHourRange(22, 24),
        AdherenceHourRange.ofHours(List.of(22, 23), TimeBand.NIGHT));
  }

  @Test
  void daytimeKeepsExistingBoundsAndRejectsUnrelatedHours() {
    assertEquals(
        new AdherenceHourRange(18, 20),
        AdherenceHourRange.ofHours(List.of(19, 18), TimeBand.EVENING));
    assertThrows(
        IllegalArgumentException.class,
        () -> AdherenceHourRange.ofHours(List.of(18, 1), TimeBand.EVENING));
    assertThrows(
        IllegalArgumentException.class,
        () -> AdherenceHourRange.ofHours(List.of(), TimeBand.NIGHT));
  }
}
