package com.vitahealth.tata.adherenceanalytics.application.queryservices;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceAnalysisResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceMetricsResult;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceInsight;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import java.util.List;

/** Public read operations for weekly adherence and immutable consolidation evidence. */
public interface AdherenceQueryService {

  /** Returns definitive intake metrics for the requested owner and period. */
  AdherenceMetricsResult weekly(GetAdherencePeriodQuery query);

  /** Returns recorded definitive outcomes without pending schedules. */
  List<IntakeOutcome> history(GetAdherencePeriodQuery query);

  /** Detects repeated omissions using the requested calendar zone. */
  List<AdherenceOmissionPattern> patterns(GetAdherencePeriodQuery query);

  /** Captures metrics and patterns from one source evidence read. */
  AdherenceAnalysisResult capture(GetAdherencePeriodQuery query);

  /** Generates follow-up insights from detected omission patterns. */
  List<AdherenceInsight> recommendations(GetAdherencePeriodQuery query);
}
