package com.tata.adherenceanalytics.application.internal.outboundservices.acl;

import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;
import com.tata.adherenceanalytics.application.models.IntakeOutcome.Status;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntakeOutcomeAdapter implements IntakeOutcomePort {
    private final IntakeContextFacade repository;
    public IntakeOutcomeAdapter(IntakeContextFacade repository) { this.repository = repository; }
    @Override public List<IntakeOutcome> find(String olderAdultId, Instant from, Instant to) {
        return repository.findEvidence(olderAdultId, from, to).stream()
                .map(intake -> new IntakeOutcome(intake.medicationId(), intake.scheduledAt(),
                        Status.valueOf(intake.status().name())))
                .toList();
    }
    @Override public List<String> olderAdultIdsWithOutcomes(Instant from, Instant to) {
        return repository.findOlderAdultIdsWithIntakes(from, to);
    }
}
