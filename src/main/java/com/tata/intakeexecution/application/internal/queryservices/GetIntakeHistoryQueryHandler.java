package com.tata.intakeexecution.application.internal.queryservices;

import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetIntakeHistoryQueryHandler {
    private final IntakeRepository repository;
    public GetIntakeHistoryQueryHandler(IntakeRepository repository) { this.repository = repository; }
    public List<IntakeResult> handle(String owner, Instant from, Instant to) {
        if (owner == null || owner.isBlank() || from == null || to == null || !to.isAfter(from)
                || Duration.between(from, to).compareTo(Duration.ofDays(30)) > 0)
            throw new IllegalArgumentException("history requires an ordered range of at most thirty days");
        return repository.findAgenda(owner.trim(), from, to).stream()
                .filter(intake -> intake.status() != IntakeStatus.PENDING)
                .map(IntakeMapper::toResult).toList();
    }
}
