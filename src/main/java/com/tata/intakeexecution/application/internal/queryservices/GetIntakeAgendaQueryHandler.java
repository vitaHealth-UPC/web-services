package com.tata.intakeexecution.application.internal.queryservices;

import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetIntakeAgendaQueryHandler {
    private final IntakeRepository repository;
    public GetIntakeAgendaQueryHandler(IntakeRepository repository) { this.repository = repository; }

    public List<IntakeResult> handle(String olderAdultId, Instant from, Instant to) {
        if (olderAdultId == null || olderAdultId.isBlank() || from == null || to == null
                || !to.isAfter(from) || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0) {
            throw new IllegalArgumentException("agenda requires an older adult and an ordered range of at most eight days");
        }
        return repository.findAgenda(olderAdultId.trim(), from, to).stream().map(IntakeMapper::toResult).toList();
    }
}
