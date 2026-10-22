package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.adapters;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeScheduleLock;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakeScheduleLockEntity;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeScheduleLockJpaRepository;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates a treatment through a database key shared by every application instance. */
@Component
public class JpaIntakeScheduleLock implements IntakeScheduleLock {
  private final IntakeScheduleLockJpaRepository reservations;
  private final PlatformTransactionManager transactions;

  /** Injects reservation persistence and explicit key-initialization transaction boundaries. */
  public JpaIntakeScheduleLock(
      IntakeScheduleLockJpaRepository reservations, PlatformTransactionManager transactions) {
    this.reservations = reservations;
    this.transactions = transactions;
  }

  /** Initializes the key separately, then locks it in the actual generation transaction. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(String treatmentId) {
    var id = requireText(treatmentId, "treatmentId");
    var initialization = new TransactionTemplate(transactions);
    initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      initialization.executeWithoutResult(
          status -> {
            if (reservations.findByTreatmentId(id).isEmpty())
              reservations.saveAndFlush(new IntakeScheduleLockEntity(id));
          });
    } catch (DataIntegrityViolationException failure) {
      if (!UniqueConstraintClassifier.matches(failure, "uk_intake_schedule_lock_treatment"))
        throw failure;
    }
    reservations
        .findForUpdate(id)
        .orElseThrow(
            () -> new IllegalStateException("schedule reservation could not be initialized"));
  }
}
