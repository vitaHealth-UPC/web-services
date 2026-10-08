package com.tata.intakeexecution.application.internal.queryservices;

import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetIntakeDetailQueryHandler implements com.tata.intakeexecution.application.queryservices.GetIntakeDetailQueryService {
    private final IntakeRepository repository;

    public GetIntakeDetailQueryHandler(IntakeRepository repository) {
        this.repository = repository;
    }

    public Optional<IntakeResult> handle(String intakeId) {
        if (intakeId == null || intakeId.isBlank()) {
            throw new IllegalArgumentException("intakeId is required");
        }
        return repository.findById(intakeId.trim())
                .map(IntakeMapper::toResult);
    }
}
