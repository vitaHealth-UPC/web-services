package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.IntakeApplicationException;
import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConfirmIntakeCommandHandler implements com.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService {
    private final IntakeRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ConfirmIntakeCommandHandler(IntakeRepository repository, ApplicationEventPublisher events) {
        this(repository, events, Clock.systemUTC());
    }

    @Autowired
    public ConfirmIntakeCommandHandler(IntakeRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    public IntakeResult handle(ConfirmIntakeCommand command) {
        var intake = repository.findByIdForConfirmation(command.intakeId())
                .orElseThrow(() -> new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND,
                        "intake not found"
                ));

        final boolean changed;
        try {
            changed = intake.confirm(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS), command.channel());
        } catch (IllegalStateException exception) {
            throw new IntakeApplicationException(
                    IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE,
                    exception.getMessage()
            );
        }

        if (changed) {
            repository.saveAll(List.of(intake));
            events.publishEvent(new IntakeConfirmed(
                    intake.id(), intake.medicationId(), intake.olderAdultId(), intake.confirmedAt()));
        }

        return IntakeMapper.toResult(intake, !changed);
    }
}
