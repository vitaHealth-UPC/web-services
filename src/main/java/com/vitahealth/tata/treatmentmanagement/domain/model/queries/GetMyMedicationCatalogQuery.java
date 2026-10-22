package com.vitahealth.tata.treatmentmanagement.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests the medication catalog for the older adult authenticated by the transport boundary. */
public record GetMyMedicationCatalogQuery(String olderAdultId) {
  /** Requires a nonblank adult identity before persistence is consulted. */
  public GetMyMedicationCatalogQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
