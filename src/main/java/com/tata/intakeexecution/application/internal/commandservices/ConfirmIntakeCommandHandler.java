package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConfirmIntakeCommandHandler {
    private final IntakeRepository repository;

    public ConfirmIntakeCommandHandler(IntakeRepository repository) {
        this.repository = repository;
    }

    public IntakeResult handle(ConfirmIntakeCommand command) {
        var intake = repository.findById(command.intakeId())
                .orElseThrow(() -> new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND,
                        "intake not found"
                ));

        final boolean changed;
        try {
            changed = intake.confirm();
        } catch (IllegalStateException exception) {
            throw new IntakeApplicationException(
                    IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE,
                    exception.getMessage()
            );
        }

        if (changed) {
            repository.saveAll(List.of(intake));
        }

        return IntakeMapper.toResult(intake);
    }
}
