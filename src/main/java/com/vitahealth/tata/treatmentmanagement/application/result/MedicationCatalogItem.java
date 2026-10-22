package com.vitahealth.tata.treatmentmanagement.application.result;

import java.util.List;

/** Catalog projection with a defensive snapshot of treatments associated with one medication. */
public record MedicationCatalogItem(MedicationResult medication, List<TreatmentResult> treatments) {
  /** Keeps the catalog independent of later collection changes in a query handler. */
  public MedicationCatalogItem {
    treatments = List.copyOf(treatments);
  }
}
