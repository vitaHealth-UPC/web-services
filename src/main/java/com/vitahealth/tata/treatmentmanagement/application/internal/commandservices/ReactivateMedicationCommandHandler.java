package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ReactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializes availability changes on the same medication lock as deactivation and configuration.
 */
@Service
@Transactional
public class ReactivateMedicationCommandHandler {
  private final MedicationRepository medications;
  private final TreatmentCareLinkGuard access;

  public ReactivateMedicationCommandHandler(
      MedicationRepository medications, TreatmentCareLinkGuard access) {
    this.medications = medications;
    this.access = access;
  }

  /**
   * Replays safely and leaves all dependent treatment states and existing intake history intact.
   */
  public MedicationResult handle(ReactivateMedicationCommand command) {
    var medication =
        medications
            .findForUpdate(command.medicationId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                        "medication not found"));
    access.requireAuthorized(command.caregiverId(), medication.olderAdultId());
    medication.reactivate();
    return TreatmentMapper.toResult(medications.save(medication));
  }
}
