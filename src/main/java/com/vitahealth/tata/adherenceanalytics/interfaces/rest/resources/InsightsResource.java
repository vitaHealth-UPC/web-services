package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Recurring issue evidence and follow-up recommendations. */
@Schema(description = "Recurring omission or lateness pattern with follow-up recommendation codes")
public record InsightsResource(
    int periodDays,
    InsightPatternResource pattern,
    List<List<Double>> concentration,
    List<String> recommendations) {
  /** Preserves the matrix and recommendation values as immutable transport data. */
  public InsightsResource {
    concentration = concentration.stream().map(List::copyOf).toList();
    recommendations = List.copyOf(recommendations);
  }
}
