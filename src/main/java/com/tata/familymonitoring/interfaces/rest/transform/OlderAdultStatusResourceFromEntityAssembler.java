package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.application.queryservices.OlderAdultStatusView;
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
            .toList(), view.weeklyAdherence());
  }
}
