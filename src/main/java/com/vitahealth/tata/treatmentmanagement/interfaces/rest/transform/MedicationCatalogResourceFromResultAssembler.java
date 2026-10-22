package com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform;

import com.vitahealth.tata.treatmentmanagement.application.result.MedicationCatalogItem;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources.MedicationCatalogResource;

/**
 * Maps MedicationCatalogItem to its HTTP representation without persistence or lifecycle decisions.
 */
public final class MedicationCatalogResourceFromResultAssembler {
  private MedicationCatalogResourceFromResultAssembler() {}

  /** Preserves the application projection fields in the public response. */
  public static MedicationCatalogResource toResource(MedicationCatalogItem result) {
    return new MedicationCatalogResource(
        MedicationResponseFromResultAssembler.toResource(result.medication()),
        result.treatments().stream()
            .map(TreatmentResponseFromResultAssembler::toResource)
            .toList());
  }
}
