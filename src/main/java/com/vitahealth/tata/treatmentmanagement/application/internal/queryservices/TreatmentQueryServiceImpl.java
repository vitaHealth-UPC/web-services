package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Captures authorized public read outcomes after their transactional query handlers finish. */
@Service
public class TreatmentQueryServiceImpl implements TreatmentQueryService {
  private final GetMedicationQueryHandler getMedication;
  private final GetTreatmentQueryHandler getTreatment;
  private final ListMedicationsQueryHandler listMedications;
  private final ListTreatmentsQueryHandler listTreatments;

  /** Injects the concrete authorized query handlers. */
  public TreatmentQueryServiceImpl(
      GetMedicationQueryHandler getMedication,
      GetTreatmentQueryHandler getTreatment,
      ListMedicationsQueryHandler listMedications,
      ListTreatmentsQueryHandler listTreatments) {
    this.getMedication = getMedication;
    this.getTreatment = getTreatment;
    this.listMedications = listMedications;
    this.listTreatments = listTreatments;
  }

  /** Delegates the authorized getMedication projection. */
  @Override
  public Result<MedicationResult, ApplicationError> handle(GetMedicationQuery query) {
    return execute(() -> getMedication.handle(query));
  }

  /** Delegates the authorized getTreatment projection. */
  @Override
  public Result<TreatmentResult, ApplicationError> handle(GetTreatmentQuery query) {
    return execute(() -> getTreatment.handle(query));
  }

  /** Delegates the authorized listMedications projection. */
  @Override
  public Result<List<MedicationResult>, ApplicationError> handle(ListMedicationsQuery query) {
    return execute(() -> listMedications.handle(query));
  }

  /** Delegates the authorized listTreatments projection. */
  @Override
  public Result<List<TreatmentResult>, ApplicationError> handle(ListTreatmentsQuery query) {
    return execute(() -> listTreatments.handle(query));
  }

  /** Captures expected rejection after the read handler transaction ends. */
  private <T> Result<T, ApplicationError> execute(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (TreatmentApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }
}
