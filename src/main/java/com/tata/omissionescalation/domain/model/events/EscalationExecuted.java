package com.tata.omissionescalation.domain.model.events;

import java.time.Instant;

public record EscalationExecuted(
    Long omissionCaseId,
    Long intakeId,
    Long olderAdultId,
    int level,
    Instant occurredAt) {
}
