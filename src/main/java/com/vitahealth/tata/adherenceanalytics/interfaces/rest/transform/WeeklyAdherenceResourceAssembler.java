package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.application.result.AdherenceMetricsResult;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.WeeklyAdherenceResource;

/** Maps captured definitive intake metrics into the weekly transport contract. */
public final class WeeklyAdherenceResourceAssembler {
  private WeeklyAdherenceResourceAssembler() {}

  /** Preserves the exact owner, period, counts and percentage returned by the query. */
  public static WeeklyAdherenceResource from(AdherenceMetricsResult metrics) {
    return new WeeklyAdherenceResource(
        metrics.olderAdultId(),
        metrics.from(),
        metrics.to(),
        metrics.confirmedIntakes(),
        metrics.totalIntakes(),
        metrics.percentage(),
        metrics.onTimeIntakes(),
        metrics.lateIntakes(),
        metrics.omittedIntakes());
  }
}
