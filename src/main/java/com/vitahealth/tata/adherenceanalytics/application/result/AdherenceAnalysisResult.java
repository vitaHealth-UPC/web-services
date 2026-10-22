package com.vitahealth.tata.adherenceanalytics.application.result;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.util.List;
import java.util.Objects;

/** Metrics and immutable pattern evidence captured from the same source read. */
public record AdherenceAnalysisResult(
    AdherenceMetricsResult metrics,
    List<AdherenceOmissionPattern> patterns,
    int minimumOmissionDays) {
  /** Retains immutable pattern evidence associated with the original metrics capture. */
  public AdherenceAnalysisResult {
    Objects.requireNonNull(metrics, "captured metrics");
    patterns = List.copyOf(patterns);
  }
}
