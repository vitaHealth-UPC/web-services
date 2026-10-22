package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verifies source evidence while retaining its row lock for a cross-context transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class IntakeEvidenceVerificationService {
  private final IntakeRepository repository;

  /** Injects the repository that refreshes evidence under a pessimistic lock. */
  public IntakeEvidenceVerificationService(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Locks the source and permits omission only while its refreshed state remains pending. */
  public boolean lockPendingIntake(String id) {
    return repository.findByIdForConfirmation(requireId(id)).filter(Intake::isPending).isPresent();
  }

  /** Locks and verifies the original definitive confirmation and its exact timestamp. */
  public boolean lockRecordedConfirmation(String id, Instant confirmedAt) {
    var intakeId = requireId(id);
    if (confirmedAt == null) return false;
    return repository
        .findByIdForConfirmation(intakeId)
        .filter(
            intake ->
                intake.status() == IntakeStatus.CONFIRMED || intake.status() == IntakeStatus.LATE)
        .filter(intake -> confirmedAt.equals(intake.confirmedAt()))
        .isPresent();
  }

  private static String requireId(String id) {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("intakeId is required");
    return id.trim();
  }
}
