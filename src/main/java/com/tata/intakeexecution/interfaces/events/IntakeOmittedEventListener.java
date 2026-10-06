package com.tata.intakeexecution.interfaces.events;

import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component("intakeExecutionOmittedEventListener")
public class IntakeOmittedEventListener {
    private final IntakeRepository repository;
    public IntakeOmittedEventListener(IntakeRepository repository) { this.repository = repository; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(IntakeOmitted event) {
        var intake = repository.findByIdForConfirmation(event.intakeId()).orElseThrow(() -> new IllegalStateException("omitted intake not found"));
        if (intake.omit()) repository.saveAll(List.of(intake));
    }
}
