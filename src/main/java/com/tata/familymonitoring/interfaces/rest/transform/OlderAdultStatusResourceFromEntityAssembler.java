package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.application.queryservices.OlderAdultStatusView;
import com.tata.familymonitoring.interfaces.rest.resources.AdherenceInsightResource;
import com.tata.familymonitoring.interfaces.rest.resources.LowStockResource;
import com.tata.familymonitoring.interfaces.rest.resources.OlderAdultStatusResource;

public final class OlderAdultStatusResourceFromEntityAssembler {

  private OlderAdultStatusResourceFromEntityAssembler() {
  }

  public static OlderAdultStatusResource toResourceFromEntity(OlderAdultStatusView view) {
    return new OlderAdultStatusResource(
        view.status().nextIntakeAt(),
        view.status().lastIntakeStatus(),
        view.status().hasOpenAlert(),
        view.openAlerts().stream()
            .map(AlertSummaryResourceFromEntityAssembler::toResourceFromEntity)
            .toList(),
        view.weeklyAdherence(),
        view.lowStockNotices().stream()
            .map(notice -> new LowStockResource(
                notice.getMedicationId(), notice.getMedicationName(), notice.getRemainingStock(),
                notice.getReplenishmentThreshold(), notice.getDetectedAt()))
            .toList(),
        view.insights().stream()
            .map(insight -> new AdherenceInsightResource(
                insight.getMedicationId(), insight.getMedicationName(), insight.getOmissionDays(),
                insight.getFirstDay(), insight.getLastDay(), insight.getDetectedAt()))
            .toList());
  }
}
