package com.tata.adherenceanalytics.infrastructure;

import com.tata.adherenceanalytics.application.ports.IntakeOutcomePort;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntakeOutcomeAdapter implements IntakeOutcomePort {
    private final IntakeRepository repository;
    public IntakeOutcomeAdapter(IntakeRepository repository) { this.repository = repository; }
    @Override public List<Outcome> find(String olderAdultId, Instant from, Instant to) {
        return repository.findAgenda(olderAdultId, from, to).stream()
                .map(intake -> new Outcome(intake.medicationId(), intake.scheduledAt(),
                        Status.valueOf(intake.status().name())))
                .toList();
    }
    @Override public List<String> olderAdultIdsWithOutcomes(Instant from, Instant to) {
        return repository.findOlderAdultIdsWithIntakes(from, to);
    }
}

