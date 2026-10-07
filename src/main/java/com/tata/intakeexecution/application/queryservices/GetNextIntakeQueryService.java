package com.tata.intakeexecution.application.queryservices;

import com.tata.intakeexecution.application.models.IntakeResult;
import java.util.Optional;

public interface GetNextIntakeQueryService {
    Optional<IntakeResult> handle(String olderAdultId);
}
