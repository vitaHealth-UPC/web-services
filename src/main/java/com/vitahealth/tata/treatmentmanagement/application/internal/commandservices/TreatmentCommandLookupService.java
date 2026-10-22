package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Resolves required treatment aggregates and validates active medication dependencies. */
@Service
public class TreatmentCommandLookupService {
  private final MedicationRepository medicationRepository;
  private final TreatmentRepository treatmentRepository;

  /** Injects the repositories that own medication and treatment lookup. */
  public TreatmentCommandLookupService(
      MedicationRepository medicationRepository, TreatmentRepository treatmentRepository) {
    this.medicationRepository = medicationRepository;
    this.treatmentRepository = treatmentRepository;
  }

  /** Locks the medication before the treatment, rejecting an association changed while waiting. */
  public Treatment treatmentForActivation(String id) {
    var snapshot = treatment(id);
    var medicationId = snapshot.regimen() == null ? null : snapshot.regimen().medicationId();
    var medication = medicationId == null ? null : medicationForUpdate(medicationId);
    var current = treatmentForUpdate(id);
    var currentMedicationId = current.regimen() == null ? null : current.regimen().medicationId();
    if (!Objects.equals(medicationId, currentMedicationId)) {
      throw error(
          TreatmentApplicationException.Code.INVALID_TRANSITION,
          "treatment medication changed; reload before retrying");
    }
    if (medication != null && !medication.active()) {
      throw error(TreatmentApplicationException.Code.MEDICATION_INACTIVE, "medication is inactive");
    }
    return current;
  }

  /** Locks the requested medication for the remainder of the caller transaction. */
  public Medication medicationForUpdate(String id) {
    return medicationRepository
        .findForUpdate(id)
        .orElseThrow(
            () ->
                error(
                    TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                    "medication not found"));
  }

  /** Loads the latest treatment under its write lock. */
  public Treatment treatmentForUpdate(String id) {
    return treatmentRepository
        .findForUpdate(id)
        .orElseThrow(
            () ->
                error(
                    TreatmentApplicationException.Code.TREATMENT_NOT_FOUND, "treatment not found"));
  }

  /** Resolves a treatment or reports its stable application error. */
  private Treatment treatment(String id) {
    return treatmentRepository
        .findById(id)
        .orElseThrow(
            () ->
                error(
                    TreatmentApplicationException.Code.TREATMENT_NOT_FOUND, "treatment not found"));
  }

  private static TreatmentApplicationException error(
      TreatmentApplicationException.Code code, String message) {
    return new TreatmentApplicationException(code, message);
  }
}
