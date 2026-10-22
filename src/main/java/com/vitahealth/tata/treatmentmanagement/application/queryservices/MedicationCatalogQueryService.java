package com.vitahealth.tata.treatmentmanagement.application.queryservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationCatalogItem;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import java.util.List;

/** Queries medication history for the adult identity derived from the authenticated session. */
public interface MedicationCatalogQueryService {
  /** Returns a successful empty or populated catalog, propagating technical failures. */
  Result<List<MedicationCatalogItem>, ApplicationError> handle(GetMyMedicationCatalogQuery query);
}
