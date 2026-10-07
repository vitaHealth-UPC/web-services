package com.tata.adherenceanalytics.infrastructure;

import com.tata.adherenceanalytics.application.ports.IntakeRecordPort;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntakeRecordAdapter implements IntakeRecordPort {
    private final IntakeRepository repository;

    public IntakeRecordAdapter(IntakeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IntakeRecord> find(String olderAdultId, Instant from, Instant to) {
        return repository.findAgenda(olderAdultId, from, to).stream()
                .map(intake -> new IntakeRecord(intake.medication().name(), intake.scheduledAt(),
                        intake.confirmedAt(), Status.valueOf(intake.status().name())))
                .toList();
    }
}
