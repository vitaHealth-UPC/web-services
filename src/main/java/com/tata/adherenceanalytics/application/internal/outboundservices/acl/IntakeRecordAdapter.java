package com.tata.adherenceanalytics.application.internal.outboundservices.acl;

import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort;
import com.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntakeRecordAdapter implements IntakeRecordPort {
    private final IntakeContextFacade repository;

    public IntakeRecordAdapter(IntakeContextFacade repository) {
        this.repository = repository;
    }

    @Override
    public List<IntakeRecord> find(String olderAdultId, Instant from, Instant to) {
        return repository.findEvidence(olderAdultId, from, to).stream()
                .map(intake -> new IntakeRecord(intake.medicationName(), intake.scheduledAt(),
                        intake.confirmedAt(), Status.valueOf(intake.status().name())))
                .toList();
    }
}
