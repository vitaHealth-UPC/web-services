package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.domain.model.commands.MarkIntakeOmittedCommand;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkIntakeOmittedCommandHandler {

    private final IntakeRepository repository;

    public MarkIntakeOmittedCommandHandler(IntakeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(MarkIntakeOmittedCommand command) {
        repository.findByIdForConfirmation(command.intakeId()).ifPresent(intake -> {
            if (intake.markOmitted()) {
                repository.saveAll(List.of(intake));
            }
        });
    }
}
