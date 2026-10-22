package com.vitahealth.tata.treatmentmanagement.application.internal.queryservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMedicationQuery;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the authorized getMedication read and its projection within a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class GetMedicationQueryHandler {
  private final MedicationRepository medications;
  private final TreatmentCareLinkGuard access;

  /** Injects context-owned persistence and current consent authorization. */
  public GetMedicationQueryHandler(
      MedicationRepository medications, TreatmentCareLinkGuard access) {
    this.medications = medications;
    this.access = access;
  }

  /** Returns the requested projection after enforcing caregiver access. */
  public MedicationResult handle(GetMedicationQuery query) {
    var medication =
        medications
            .findById(query.medicationId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                        "medication not found"));
    access.requireAuthorized(query.caregiverId(), medication.olderAdultId());
    return TreatmentMapper.toResult(medication);
  }
}
