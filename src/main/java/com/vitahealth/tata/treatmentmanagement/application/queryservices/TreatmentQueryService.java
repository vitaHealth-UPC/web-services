package com.vitahealth.tata.treatmentmanagement.application.queryservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import java.util.List;

/** Exposes typed authorized caregiver queries with explicit expected failure outcomes. */
public interface TreatmentQueryService {
  /** Returns the getMedication projection after enforcing current consented access. */
  Result<MedicationResult, ApplicationError> handle(GetMedicationQuery query);

  /** Returns the getTreatment projection after enforcing current consented access. */
  Result<TreatmentResult, ApplicationError> handle(GetTreatmentQuery query);

  /** Returns the listMedications projection after enforcing current consented access. */
  Result<List<MedicationResult>, ApplicationError> handle(ListMedicationsQuery query);

  /** Returns the listTreatments projection after enforcing current consented access. */
  Result<List<TreatmentResult>, ApplicationError> handle(ListTreatmentsQuery query);
}
