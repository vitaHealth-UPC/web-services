package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentScheduleChangedEventAssembler;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ExtendTreatmentScheduleCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Publishes one current active schedule under the same lock order as caregiver mutations. */
@Service
public class ExtendTreatmentScheduleCommandHandler {
  private final TreatmentRepository treatments;
  private final MedicationRepository medications;
  private final TreatmentScheduleEventPublisher publisher;

  /** Injects context-owned aggregate reads and synchronous schedule delivery. */
  public ExtendTreatmentScheduleCommandHandler(
      TreatmentRepository treatments,
      MedicationRepository medications,
      TreatmentScheduleEventPublisher publisher) {
    this.treatments = treatments;
    this.medications = medications;
    this.publisher = publisher;
  }

  /** Commits or rolls back this candidate independently of any surrounding batch. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(ExtendTreatmentScheduleCommand command) {
    var snapshot = treatments.findById(command.treatmentId()).orElse(null);
    if (snapshot == null
        || snapshot.regimen() == null
        || snapshot.status() != TreatmentStatus.ACTIVE) return false;
    var medicationId = snapshot.regimen().medicationId();
    var medication = medications.findForUpdate(medicationId).orElse(null);
    if (medication == null || !medication.active()) return false;
    var current = treatments.findForUpdate(command.treatmentId()).orElse(null);
    if (current == null
        || current.status() != TreatmentStatus.ACTIVE
        || current.regimen() == null
        || !medicationId.equals(current.regimen().medicationId())) return false;
    publisher.publish(TreatmentScheduleChangedEventAssembler.toEvent(current, medication));
    return true;
  }
}
