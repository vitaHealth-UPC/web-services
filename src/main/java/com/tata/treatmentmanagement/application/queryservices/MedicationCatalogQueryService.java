package com.tata.treatmentmanagement.application.queryservices;

import com.tata.treatmentmanagement.application.models.MedicationCatalogItem;
import com.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import java.util.List;

public interface MedicationCatalogQueryService {
    List<MedicationCatalogItem> handle(GetMyMedicationCatalogQuery query);
}
