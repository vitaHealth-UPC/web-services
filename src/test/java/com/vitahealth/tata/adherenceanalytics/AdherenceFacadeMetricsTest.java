package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade.WeeklyMetrics;
import org.junit.jupiter.api.Test;

/** Checks the evidence invariant shared with Family Monitoring. */
class AdherenceFacadeMetricsTest {
  @Test
  void acceptsEmptyAndFullyConfirmedEvidence() {
    assertEquals(0, new WeeklyMetrics(0, 0).totalIntakes());
    assertEquals(3, new WeeklyMetrics(3, 3).confirmedIntakes());
  }

  @Test
  void rejectsNegativeAndContradictoryCounts() {
    assertThrows(IllegalArgumentException.class, () -> new WeeklyMetrics(-1, 3));
    assertThrows(IllegalArgumentException.class, () -> new WeeklyMetrics(0, -1));
    assertThrows(IllegalArgumentException.class, () -> new WeeklyMetrics(4, 3));
  }
}
