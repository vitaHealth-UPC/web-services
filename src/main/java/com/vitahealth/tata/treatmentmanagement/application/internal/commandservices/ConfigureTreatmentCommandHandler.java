package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentSchedulePublicationService;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRecurrence;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the authorized configureTreatment transition and publishes its schedule atomically. */
@Service
@Transactional
public class ConfigureTreatmentCommandHandler {
  private final TreatmentCommandLookupService lookup;
  private final TreatmentRepository treatmentRepository;
  private final TreatmentCareLinkGuard access;
  private final TreatmentSchedulePublicationService schedules;

  /** Injects aggregate lookup, persistence, authorization and schedule publication. */
  public ConfigureTreatmentCommandHandler(
      TreatmentCommandLookupService lookup,
      TreatmentRepository treatmentRepository,
      TreatmentCareLinkGuard access,
      TreatmentSchedulePublicationService schedules) {
    this.lookup = lookup;
    this.treatmentRepository = treatmentRepository;
    this.access = access;
    this.schedules = schedules;
  }

  /** Applies the command inside the handler transaction. */
  public TreatmentResult handle(ConfigureTreatmentCommand command) {
    var medication = lookup.medicationForUpdate(command.medicationId());
    var treatment = lookup.treatmentForUpdate(command.treatmentId());
    access.requireAuthorized(command.caregiverId(), treatment.olderAdultId());
    if (!medication.active()) {
      throw error(TreatmentApplicationException.Code.MEDICATION_INACTIVE, "medication is inactive");
    }
    if (!medication.olderAdultId().equals(treatment.olderAdultId())) {
      throw new IllegalArgumentException(
          "medication and treatment must belong to the same older adult");
    }
    boolean recurrenceOmitted =
        command.recurrenceMode() == null
            && command.recurrenceZone() == null
            && command.recurrenceAnchor() == null;
    var recurrence =
        recurrenceOmitted && treatment.regimen() != null
            ? treatment.regimen().recurrence()
            : TreatmentRecurrence.fromMetadata(
                command.recurrenceMode(), command.recurrenceZone(), command.recurrenceAnchor());
    treatment.configure(
        new TreatmentRegimen(
            command.medicationId(),
            command.dose(),
            command.frequency(),
            command.scheduledTimes(),
            command.instructions(),
            command.reminderLeadMinutes(),
            recurrence,
            command.followUpDelayMinutes()));
    var saved = treatmentRepository.save(treatment);
    schedules.publish(saved);
    return TreatmentMapper.toResult(saved);
  }

  private static TreatmentApplicationException error(
      TreatmentApplicationException.Code code, String message) {
    return new TreatmentApplicationException(code, message);
  }
}
