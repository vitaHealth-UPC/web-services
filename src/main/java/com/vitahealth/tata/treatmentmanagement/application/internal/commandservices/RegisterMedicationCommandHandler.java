package com.vitahealth.tata.treatmentmanagement.application.internal.commandservices;

import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentMapper;
import com.vitahealth.tata.treatmentmanagement.application.internal.authorization.TreatmentCareLinkGuard;
import com.vitahealth.tata.treatmentmanagement.application.result.MedicationResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns authorized medication registration and its transaction. */
@Service
@Transactional
public class RegisterMedicationCommandHandler {
  private final MedicationRepository repository;
  private final TreatmentCareLinkGuard access;
  private final Clock clock;

  /** Injects registration persistence, live consent authorization and occurrence time. */
  public RegisterMedicationCommandHandler(
      MedicationRepository repository, TreatmentCareLinkGuard access, Clock clock) {
    this.repository = repository;
    this.access = access;
    this.clock = clock;
  }

  /** Registers new evidence only after the caregiver relationship is authorized. */
  public MedicationResult handle(RegisterMedicationCommand command) {
    access.requireAuthorized(command.caregiverId(), command.olderAdultId());
    return TreatmentMapper.toResult(
        repository.save(
            Medication.register(
                command.olderAdultId(), command.name(), command.presentation(), clock.instant())));
  }
}
