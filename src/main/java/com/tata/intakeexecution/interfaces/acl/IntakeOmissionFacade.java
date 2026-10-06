package com.tata.intakeexecution.interfaces.acl;

import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Lock order is intake first, then omission case, for both confirmation and omission. */
@Service
public class IntakeOmissionFacade {
    private final IntakeRepository repository;
    public IntakeOmissionFacade(IntakeRepository repository) { this.repository = repository; }
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockPendingIntake(String intakeId) {
        return repository.findByIdForConfirmation(intakeId).map(intake -> intake.isPending()).orElse(false);
    }
}
