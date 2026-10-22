package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetTreatmentQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the authorized getTreatment read and its projection within a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class GetTreatmentQueryHandler {
  private final TreatmentRepository treatments;
  private final TreatmentCareLinkGuard access;

  /** Injects context-owned persistence and current consent authorization. */
  public GetTreatmentQueryHandler(TreatmentRepository treatments, TreatmentCareLinkGuard access) {
    this.treatments = treatments;
    this.access = access;
  }

  /** Returns the requested projection after enforcing caregiver access. */
  public TreatmentResult handle(GetTreatmentQuery query) {
    var treatment =
        treatments
            .findById(query.treatmentId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.TREATMENT_NOT_FOUND,
                        "treatment not found"));
    access.requireAuthorized(query.caregiverId(), treatment.olderAdultId());
    return TreatmentMapper.toResult(treatment);
  }
}
