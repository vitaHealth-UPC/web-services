package com.vitahealth.tata.adherenceanalytics.application.result;

import java.util.List;

/** Immutable concentration matrix and recommendations for the requested period. */
public record AdherenceInsightsResult(
    int periodDays,
    AdherenceInsightPatternResult pattern,
    List<List<Double>> concentration,
    List<String> recommendations) {
  /** Copies both matrix levels and recommendation text references. */
  public AdherenceInsightsResult {
    concentration = concentration.stream().map(List::copyOf).toList();
    recommendations = List.copyOf(recommendations);
  }
}
