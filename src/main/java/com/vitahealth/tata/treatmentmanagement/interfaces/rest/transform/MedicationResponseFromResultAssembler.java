package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.MedicationResponse;

/** Maps MedicationResult to its HTTP representation without persistence or lifecycle decisions. */
public final class MedicationResponseFromResultAssembler {
  private MedicationResponseFromResultAssembler() {}

  /** Preserves the application projection fields in the public response. */
  public static MedicationResponse toResource(MedicationResult result) {
    return new MedicationResponse(
        result.id(), result.olderAdultId(), result.name(), result.presentation(), result.active());
  }
}
