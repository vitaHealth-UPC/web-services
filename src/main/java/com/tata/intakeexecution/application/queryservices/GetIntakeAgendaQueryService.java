package com.tata.intakeexecution.application.queryservices;

import com.tata.intakeexecution.application.models.IntakeResult;
import java.time.Instant;
import java.util.List;

public interface GetIntakeAgendaQueryService {
    List<IntakeResult> handle(String olderAdultId, Instant from, Instant to);
}
