package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.application.commands.ReportUnconfirmedIntakesCommand;
import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportUnconfirmedIntakesCommandHandler {

    private final IntakeRepository repository;
    private final ApplicationEventPublisher events;

    public ReportUnconfirmedIntakesCommandHandler(
            IntakeRepository repository,
            ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.events = events;
    }

    /**
     * Publishes the cross-context outcome once for every pending intake that has
     * remained unresolved through the configured initial waiting period.
     */
    @Transactional
    public int handle(ReportUnconfirmedIntakesCommand command) {
        if (command.cutoff() == null || command.reportedAt() == null) {
            throw new IllegalArgumentException("cutoff and reportedAt are required");
        }

        int reported = 0;
        for (var intake : repository.findUnreportedPendingDue(command.cutoff())) {
            if (!intake.reportUnconfirmed(command.reportedAt())) {
                continue;
            }

            repository.saveAll(List.of(intake));
            events.publishEvent(new IntakeUnconfirmed(
                    intake.id(),
                    intake.olderAdultId(),
                    intake.medication().name(),
                    intake.scheduledAt()
            ));
            reported++;
        }
        return reported;
    }
}