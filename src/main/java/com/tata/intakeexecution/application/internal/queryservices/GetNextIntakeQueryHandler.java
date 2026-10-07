package com.tata.intakeexecution.application.internal.queryservices;

import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetNextIntakeQueryHandler implements com.tata.intakeexecution.application.queryservices.GetNextIntakeQueryService {
    private final IntakeRepository repository;
    private final Clock clock = Clock.systemUTC();

    public GetNextIntakeQueryHandler(IntakeRepository repository) {
        this.repository = repository;
    }

    public Optional<IntakeResult> handle(String olderAdultId) {
        if (olderAdultId == null || olderAdultId.isBlank()) {
            throw new IllegalArgumentException("olderAdultId is required");
        }
        return repository.findNextPendingByOlderAdultId(olderAdultId.trim(), clock.instant())
                .map(IntakeMapper::toResult);
    }
}
