package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentSchedulePublicationService;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the authorized activate transition and publishes its schedule atomically. */
@Service
@Transactional
public class ActivateTreatmentCommandHandler {
  private final TreatmentCommandLookupService lookup;
  private final TreatmentRepository treatmentRepository;
  private final TreatmentCareLinkGuard access;
  private final TreatmentSchedulePublicationService schedules;

  /** Injects aggregate lookup, persistence, authorization and schedule publication. */
  public ActivateTreatmentCommandHandler(
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
  public TreatmentResult handle(ChangeTreatmentStatusCommand command) {
    var treatment = lookup.treatmentForActivation(command.treatmentId());
    access.requireAuthorized(command.caregiverId(), treatment.olderAdultId());
    try {
      treatment.activate();
    } catch (IllegalStateException exception) {
      throw error(TreatmentApplicationException.Code.INCOMPLETE_TREATMENT, exception.getMessage());
    }
    var saved = treatmentRepository.save(treatment);
    schedules.publish(saved);
    return TreatmentMapper.toResult(saved);
  }

  private static TreatmentApplicationException error(
      TreatmentApplicationException.Code code, String message) {
    return new TreatmentApplicationException(code, message);
  }
}
