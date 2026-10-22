package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentSchedulePublicationService;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns authorized medication edits and publication of the dependent schedule descriptions. */
@Service
@Transactional
public class UpdateMedicationCommandHandler {
  private final MedicationRepository medications;
  private final TreatmentRepository treatments;
  private final TreatmentCareLinkGuard access;
  private final TreatmentSchedulePublicationService schedules;

  /**
   * Injects medication writes, dependent treatment reads, authorization and transactional
   * publication.
   */
  public UpdateMedicationCommandHandler(
      MedicationRepository medications,
      TreatmentRepository treatments,
      TreatmentCareLinkGuard access,
      TreatmentSchedulePublicationService schedules) {
    this.medications = medications;
    this.treatments = treatments;
    this.access = access;
    this.schedules = schedules;
  }

  /** Validates an active medication and republishes its changed name before committing. */
  public MedicationResult handle(UpdateMedicationCommand command) {
    var medication =
        medications
            .findForUpdate(command.medicationId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                        "medication not found"));
    access.requireAuthorized(command.caregiverId(), medication.olderAdultId());
    try {
      medication.update(command.name(), command.presentation());
    } catch (IllegalStateException failure) {
      throw new TreatmentApplicationException(
          TreatmentApplicationException.Code.MEDICATION_INACTIVE, failure.getMessage());
    }
    var saved = medications.save(medication);
    var dependentIds =
        treatments.findByMedicationId(saved.id()).stream().map(t -> t.id()).sorted().toList();
    for (var id : dependentIds) {
      var treatment = treatments.findForUpdate(id).orElseThrow();
      if (treatment.regimen() != null && saved.id().equals(treatment.regimen().medicationId()))
        schedules.publish(treatment);
    }
    return TreatmentMapper.toResult(saved);
  }
}
