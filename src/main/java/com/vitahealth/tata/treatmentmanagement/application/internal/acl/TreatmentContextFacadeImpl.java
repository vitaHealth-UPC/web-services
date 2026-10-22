package com.vitahealth.tata.treatmentmanagement.application.internal.acl;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.TreatmentIntegrationQueryService;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListMedicationsQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.ListTreatmentsQuery;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Exposes Treatment Management contracts while delegating all data access to application queries.
 */
@Service
public class TreatmentContextFacadeImpl implements TreatmentContextFacade {
  private final TreatmentQueryService queries;
  private final TreatmentIntegrationQueryService integration;

  /** Injects authorized public reads and transactional integration evidence reads. */
  public TreatmentContextFacadeImpl(
      TreatmentQueryService queries, TreatmentIntegrationQueryService integration) {
    this.queries = queries;
    this.integration = integration;
  }

  /** Reads authorized medication details. */
  @Override
  public MedicationResult getMedication(String caregiverId, String medicationId) {
    return requireValue(queries.handle(new GetMedicationQuery(caregiverId, medicationId)));
  }

  /** Reads authorized treatment details. */
  @Override
  public TreatmentResult getTreatment(String caregiverId, String treatmentId) {
    return requireValue(queries.handle(new GetTreatmentQuery(caregiverId, treatmentId)));
  }

  /** Lists medications within the authorized adult profile. */
  @Override
  public List<MedicationResult> listMedications(String caregiverId, String olderAdultId) {
    return requireValue(queries.handle(new ListMedicationsQuery(caregiverId, olderAdultId)));
  }

  /** Lists treatments within the authorized adult profile. */
  @Override
  public List<TreatmentResult> listTreatments(String caregiverId, String olderAdultId) {
    return requireValue(queries.handle(new ListTreatmentsQuery(caregiverId, olderAdultId)));
  }

  /** Delegates the trusted event lookup to a read-only application service. */
  @Override
  public Optional<TreatmentResult> findTreatment(String id) {
    return integration.findTreatment(id);
  }

  /** Delegates the trusted medication lookup without performing persistence in the facade. */
  @Override
  public Optional<MedicationResult> findMedication(String id) {
    return integration.findMedication(id);
  }

  /** Reserves the source medication through its application-owned transaction boundary. */
  @Override
  public Optional<MedicationResult> reserveMedication(String medicationId) {
    return integration.reserveMedication(medicationId);
  }

  /** Delegates bounded reference discovery to the transactional integration query service. */
  @Override
  public List<String> findMedicationIdsAfter(String olderAdultId, String afterId, int limit) {
    return integration.findMedicationIdsAfter(olderAdultId, afterId, limit);
  }

  /** Preserves the legacy integration contract's scheduled occurrence count. */
  @Override
  public Optional<Integer> scheduledDailyUnits(String id) {
    return integration.scheduledDailyOccurrences(id);
  }

  /**
   * Translates expected failures for the existing cross-context contract, preserving technical
   * failures.
   */
  private static <T> T requireValue(Result<T, ApplicationError> result) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success -> success.value();
      case Result.Failure<T, ApplicationError> failure ->
          throw new TreatmentApplicationException(
              TreatmentApplicationException.Code.valueOf(failure.error().code()),
              failure.error().message());
    };
  }
}
