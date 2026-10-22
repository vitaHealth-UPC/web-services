package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.AlertSummaryResource;

/** Maps immutable application evidence to the existing REST resource contract. */
public final class AlertSummaryResourceFromResultAssembler {

  private AlertSummaryResourceFromResultAssembler() {}

  /** Copies response fields without authorization, persistence or business transitions. */
  public static AlertSummaryResource toResourceFromResult(AlertResult alert) {
    return new AlertSummaryResource(
        alert.id(),
        alert.intakeId(),
        alert.medicationName(),
        alert.scheduledAt(),
        alert.reason(),
        alert.status(),
        alert.openedAt(),
        alert.closedAt());
  }
}
