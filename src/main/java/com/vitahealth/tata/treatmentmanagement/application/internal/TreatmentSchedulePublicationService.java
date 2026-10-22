package com.vitahealth.tata.treatmentmanagement.application.internal;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads medication evidence and publishes a configured treatment snapshot in the caller
 * transaction.
 */
@Service
public class TreatmentSchedulePublicationService {
  private final MedicationRepository medications;
  private final TreatmentScheduleEventPublisher events;

  /** Injects context medication reads and the integration publication contract. */
  public TreatmentSchedulePublicationService(
      MedicationRepository medications, TreatmentScheduleEventPublisher events) {
    this.medications = medications;
    this.events = events;
  }

  /**
   * Leaves an unconfigured draft without a schedule; propagates missing or failed delivery
   * evidence.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(Treatment treatment) {
    if (treatment.regimen() == null) return;
    var medication =
        medications
            .findById(treatment.regimen().medicationId())
            .orElseThrow(
                () ->
                    new TreatmentApplicationException(
                        TreatmentApplicationException.Code.MEDICATION_NOT_FOUND,
                        "medication not found"));
    events.publish(TreatmentScheduleChangedEventAssembler.toEvent(treatment, medication));
  }
}
