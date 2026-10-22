package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceInsightsResult;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.InsightPatternResource;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.InsightsResource;

/** Maps recurring issue results into the public insights resource. */
public final class AdherenceInsightsResourceAssembler {
  private AdherenceInsightsResourceAssembler() {}

  /** Preserves all result values while mapping domain labels to transport strings. */
  public static InsightsResource from(AdherenceInsightsResult insights) {
    var pattern = insights.pattern();
    return new InsightsResource(
        insights.periodDays(),
        new InsightPatternResource(
            pattern.type(),
            pattern.timeBand().name(),
            pattern.omittedCount(),
            pattern.lateCount(),
            pattern.fromHour(),
            pattern.toHour()),
        insights.concentration(),
        insights.recommendations());
  }
}
