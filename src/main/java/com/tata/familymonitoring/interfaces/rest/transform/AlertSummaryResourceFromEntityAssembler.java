package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.interfaces.rest.resources.AlertSummaryResource;

public final class AlertSummaryResourceFromEntityAssembler {

  private AlertSummaryResourceFromEntityAssembler() {
  }

  public static AlertSummaryResource toResourceFromEntity(AlertSummary alert) {
    return new AlertSummaryResource(
        alert.getId(),
        alert.getIntakeId(),
        alert.getMedicationName(),
        alert.getScheduledAt(),
        alert.getReason(),
        alert.getStatus(),
        alert.getOpenedAt(),
        alert.getClosedAt());
  }
}
