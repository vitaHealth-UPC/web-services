package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns authorized treatment registration and its transaction. */
@Service
@Transactional
public class CreateTreatmentCommandHandler {
  private final TreatmentRepository repository;
  private final TreatmentCareLinkGuard access;
  private final Clock clock;

  /** Injects registration persistence, live consent authorization and occurrence time. */
  public CreateTreatmentCommandHandler(
      TreatmentRepository repository, TreatmentCareLinkGuard access, Clock clock) {
    this.repository = repository;
    this.access = access;
    this.clock = clock;
  }

  /** Registers new evidence only after the caregiver relationship is authorized. */
  public TreatmentResult handle(CreateTreatmentCommand command) {
    access.requireAuthorized(command.caregiverId(), command.olderAdultId());
    var planId = command.planId() == null || command.planId().isBlank() ? null : command.planId();
    if (planId != null) {
      var plan =
          repository
              .findById(planId)
              .filter(existing -> existing.olderAdultId().equals(command.olderAdultId()))
              .orElseThrow(
                  () ->
                      new TreatmentApplicationException(
                          TreatmentApplicationException.Code.TREATMENT_NOT_FOUND,
                          "treatment plan not found"));
      planId = plan.planId();
    }
    return TreatmentMapper.toResult(
        repository.save(
            Treatment.create(command.olderAdultId(), command.name(), planId, clock.instant())));
  }
}
