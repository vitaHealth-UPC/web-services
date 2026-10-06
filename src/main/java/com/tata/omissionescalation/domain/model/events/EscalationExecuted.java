package com.tata.omissionescalation.domain.model.events;

import java.time.Instant;

public record EscalationExecuted(
    Long omissionCaseId,
    String intakeId,
    String olderAdultId,
    int level,
    Instant occurredAt) {
}
