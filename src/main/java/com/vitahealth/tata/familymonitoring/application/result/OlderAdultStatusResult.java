package com.vitahealth.tata.familymonitoring.application.result;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.OlderAdultStatus;
import java.util.List;

/**
 * Result of the status query: the status itself, the alerts awaiting explicit closure, the weekly
 * adherence, the medications running out and the latest adherence patterns.
 */
public record OlderAdultStatusResult(
    OlderAdultStatus status,
    List<AlertResult> openAlerts,
    AdherenceSnapshot weeklyAdherence,
    List<LowStockNoticeResult> lowStockNotices,
    List<AdherenceInsightResult> insights) {
  /** Copies result collections so caller mutation cannot change the captured response. */
  public OlderAdultStatusResult {
    openAlerts = List.copyOf(openAlerts);
    lowStockNotices = List.copyOf(lowStockNotices);
    insights = List.copyOf(insights);
  }
}
