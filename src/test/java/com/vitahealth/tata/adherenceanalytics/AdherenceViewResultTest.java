package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceAnalysisResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceInsightsResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceMetricsResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceSummaryResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceTrendPointResult;
import com.vitahealth.tata.adherenceanalytics.application.result.RecentAdherenceIntakeResult;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protects completed query results from subsequent mutation by producers or consumers. */
class AdherenceViewResultTest {
  @Test
  void summaryCopiesItsSourceCollectionsAndExposesImmutableLists() {
    var trend = new ArrayList<AdherenceTrendPointResult>();
    trend.add(new AdherenceTrendPointResult(LocalDate.parse("2026-10-01"), 75));
    var recent = new ArrayList<RecentAdherenceIntakeResult>();
    var result = new AdherenceSummaryResult(7, 4, 75, null, 50, null, 1, 1, trend, recent, null);
    trend.clear();
    assertEquals(1, result.trend().size());
    assertThrows(UnsupportedOperationException.class, () -> result.trend().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.recentIntakes().clear());
  }

  @Test
  void insightsCopiesBothMatrixLevelsAndRecommendations() {
    var row = new ArrayList<>(List.of(0.25, 0.75));
    var matrix = new ArrayList<List<Double>>();
    matrix.add(row);
    var recommendations = new ArrayList<>(List.of("Revisar la rutina"));
    var result = new AdherenceInsightsResult(7, null, matrix, recommendations);
    row.set(0, 1.0);
    matrix.clear();
    recommendations.clear();
    assertEquals(List.of(List.of(0.25, 0.75)), result.concentration());
    assertEquals(List.of("Revisar la rutina"), result.recommendations());
    assertThrows(UnsupportedOperationException.class, () -> result.concentration().clear());
    assertThrows(
        UnsupportedOperationException.class, () -> result.concentration().getFirst().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.recommendations().clear());
  }

  @Test
  void analysisRetainsItsOriginalPatternEvidence() {
    var from = Instant.parse("2026-10-01T00:00:00Z");
    var metrics =
        new AdherenceMetricsResult("adult", from, from.plusSeconds(604800), 0, 3, 0, 0, 0, 3);
    var patterns = new ArrayList<AdherenceOmissionPattern>();
    patterns.add(
        new AdherenceOmissionPattern(
            "medication", 3, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03")));
    var result = new AdherenceAnalysisResult(metrics, patterns, 3);
    patterns.clear();
    assertEquals(1, result.patterns().size());
    assertEquals(metrics, result.metrics());
    assertThrows(UnsupportedOperationException.class, () -> result.patterns().clear());
  }
}
