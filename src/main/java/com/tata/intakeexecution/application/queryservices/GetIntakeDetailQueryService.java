package com.tata.intakeexecution.application.queryservices;

import com.tata.intakeexecution.application.models.IntakeResult;
import java.util.Optional;

public interface GetIntakeDetailQueryService {
    Optional<IntakeResult> handle(String intakeId);
}
