package com.vitahealth.tata.adherenceanalytics.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdherencePeriodSnapshotTest {
  private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
  private final Instant to = from.plusSeconds(604800);

  @Test
  void rejectsInvalidPeriodAndCounts() {
    assertThrows(IllegalArgumentException.class, () -> snapshot(from, -1, List.of()));
    assertThrows(IllegalArgumentException.class, () -> snapshot(to, -1, List.of()));
  }

  @Test
  void preservesMetricsAndCopiesPatternEvidence() {
    var patterns = new ArrayList<AdherenceOmissionPattern>();
    patterns.add(
        new AdherenceOmissionPattern(
            "med-1", 2, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));
    var snapshot = snapshot(to, 3, patterns);
    patterns.clear();
    assertEquals(1, snapshot.patterns().size());
    assertThrows(UnsupportedOperationException.class, () -> snapshot.patterns().clear());
    assertEquals(4, snapshot.confirmedIntakes());
    assertEquals(6, snapshot.totalIntakes());
    assertEquals(4 * 100d / 6, snapshot.percentage());
  }

  @Test
  void rejectsDuplicateOutsidePeriodOrUnsupportedPatternEvidence() {
    var valid =
        new AdherenceOmissionPattern(
            "med-1", 2, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2));
    assertThrows(IllegalArgumentException.class, () -> snapshot(to, 0, List.of(valid, valid)));
    var outside =
        new AdherenceOmissionPattern(
            "med-1", 2, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29));
    assertThrows(IllegalArgumentException.class, () -> snapshot(to, 0, List.of(outside)));
    var excess =
        new AdherenceOmissionPattern(
            "med-1", 3, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    assertThrows(IllegalArgumentException.class, () -> snapshot(to, 0, List.of(excess)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AdherenceOmissionPattern(
                "med-1", 3, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));
  }

  private AdherencePeriodSnapshot snapshot(
      Instant end, int onTime, List<AdherenceOmissionPattern> patterns) {
    return new AdherencePeriodSnapshot(
        "period-1", "adult-1", from, end, "America/Bogota", onTime, 1, 2, patterns, 2, to);
  }
}
