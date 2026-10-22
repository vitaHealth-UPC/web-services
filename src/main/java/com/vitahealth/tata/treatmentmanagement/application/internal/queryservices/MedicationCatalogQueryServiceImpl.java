package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationCatalogItem;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import java.util.List;
import org.springframework.stereotype.Service;

/** Exposes the authenticated adult catalog as an explicit successful application value. */
@Service
public class MedicationCatalogQueryServiceImpl implements MedicationCatalogQueryService {
  private final GetMyMedicationCatalogQueryHandler catalog;

  /** Injects the read-only catalog handler; technical read failures propagate. */
  public MedicationCatalogQueryServiceImpl(GetMyMedicationCatalogQueryHandler catalog) {
    this.catalog = catalog;
  }

  /**
   * Preserves empty lists as successful evidence without converting technical failures to absence.
   */
  @Override
  public Result<List<MedicationCatalogItem>, ApplicationError> handle(
      GetMyMedicationCatalogQuery query) {
    return Result.success(catalog.handle(query));
  }
}
