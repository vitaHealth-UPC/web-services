package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.TreatmentResponse;

/** Maps TreatmentResult to its HTTP representation without persistence or lifecycle decisions. */
public final class TreatmentResponseFromResultAssembler {
  private TreatmentResponseFromResultAssembler() {}

  /** Preserves the application projection fields in the public response. */
  public static TreatmentResponse toResource(TreatmentResult result) {
    return new TreatmentResponse(
        result.id(),
        result.olderAdultId(),
        result.name(),
        result.status().name(),
        result.medicationId(),
        result.dose(),
        result.frequency(),
        result.scheduledTimes(),
        result.instructions(),
        result.reminderLeadMinutes(),
        result.recurrenceMode(),
        result.recurrenceZone(),
        result.recurrenceAnchor());
  }
}
