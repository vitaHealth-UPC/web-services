package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class ReportUnconfirmedIntakeCommandHandler {
    private final IntakeRepository repository;
    private final ApplicationEventPublisher events;
    public ReportUnconfirmedIntakeCommandHandler(IntakeRepository repository, ApplicationEventPublisher events) {
        this.repository = repository; this.events = events;
    }
    @Transactional
    public void handle(String intakeId, Instant now) {
        var intake = repository.findByIdForConfirmation(intakeId).orElse(null);
        if (intake == null || !intake.reportUnconfirmed(now)) return;
        repository.saveAll(List.of(intake));
        events.publishEvent(new IntakeUnconfirmed(intake.id(), intake.olderAdultId(), intake.medication().name(), intake.scheduledAt()));
    }
}
