package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentSchedulePublicationService;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deactivates medication and pauses its active treatments in one transaction. */
@Service
@Transactional
public class DeactivateMedicationCommandHandler {
  private final MedicationRepository medications;
  private final TreatmentRepository treatments;
  private final TreatmentCareLinkGuard access;
  private final TreatmentSchedulePublicationService schedules;

  /** Injects aggregate persistence, current consent authorization and schedule delivery. */
  public DeactivateMedicationCommandHandler(
      MedicationRepository medications,
      TreatmentRepository treatments,
      TreatmentCareLinkGuard access,
      TreatmentSchedulePublicationService schedules) {
    this.medications = medications;
    this.treatments = treatments;
    this.access = access;
    this.schedules = schedules;
  }

  /** Keeps drafts and paused history intact while stopping all dependent active schedules. */
  public MedicationResult handle(DeactivateMedicationCommand command) {
    var medication =
        medications
            .findForUpdate(command.medicationId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                        "medication not found"));
    access.requireAuthorized(command.caregiverId(), medication.olderAdultId());
    medication.deactivate();
    var saved = medications.save(medication);
    var dependentIds =
        treatments.findByMedicationId(saved.id()).stream().map(t -> t.id()).sorted().toList();
    for (var id : dependentIds) {
      var treatment = treatments.findForUpdate(id).orElseThrow();
      if (treatment.regimen() == null || !saved.id().equals(treatment.regimen().medicationId()))
        continue;
      if (treatment.status() == TreatmentStatus.ACTIVE) {
        treatment.pause();
        schedules.publish(treatments.save(treatment));
      }
    }
    return TreatmentMapper.toResult(saved);
  }
}
