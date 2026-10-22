package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.application.result.OlderAdultStatusResult;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.AdherenceInsightResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.LowStockResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.OlderAdultStatusResource;

/** Maps immutable application evidence to the existing REST resource contract. */
public final class OlderAdultStatusResourceFromResultAssembler {

  private OlderAdultStatusResourceFromResultAssembler() {}

  /** Copies response fields without authorization, persistence or business transitions. */
  public static OlderAdultStatusResource toResourceFromResult(OlderAdultStatusResult view) {
    return new OlderAdultStatusResource(
        view.status().nextIntakeAt(),
        view.status().lastIntakeStatus(),
        view.status().hasOpenAlert(),
        view.openAlerts().stream()
            .map(AlertSummaryResourceFromResultAssembler::toResourceFromResult)
            .toList(),
        view.weeklyAdherence(),
        view.lowStockNotices().stream()
            .map(
                notice ->
                    new LowStockResource(
                        notice.medicationId(),
                        notice.medicationName(),
                        notice.remainingStock(),
                        notice.replenishmentThreshold(),
                        notice.detectedAt()))
            .toList(),
        view.insights().stream()
            .map(
                insight ->
                    new AdherenceInsightResource(
                        insight.medicationId(),
                        insight.medicationName(),
                        insight.omissionDays(),
                        insight.firstDay(),
                        insight.lastDay(),
                        insight.detectedAt()))
            .toList());
  }
}
