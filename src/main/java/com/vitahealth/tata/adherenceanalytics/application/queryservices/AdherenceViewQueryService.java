package com.vitahealth.tata.adherenceanalytics.application.queryservices;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceInsightsResult;
import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceSummaryResult;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceViewQuery;
import java.util.Optional;

/** Public read boundary for adherence history and evidence-based recommendations. */
public interface AdherenceViewQueryService {

  /** Returns history metrics when the requested period contains definitive evidence. */
  Optional<AdherenceSummaryResult> summary(GetAdherenceViewQuery query);

  /** Returns recommendations only when sufficient issue evidence exists. */
  Optional<AdherenceInsightsResult> insights(GetAdherenceViewQuery query);
}
