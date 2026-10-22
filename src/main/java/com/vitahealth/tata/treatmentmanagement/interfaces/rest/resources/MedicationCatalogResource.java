package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Groups one adult-owned medication with its associated treatment representations. */
@Schema(description = "Medication and associated treatments of the authenticated older adult")
public record MedicationCatalogResource(
    MedicationResponse medication, List<TreatmentResponse> treatments) {
  /** Snapshots associated treatments so callers cannot mutate the response collection. */
  public MedicationCatalogResource {
    treatments = List.copyOf(treatments);
  }
}
