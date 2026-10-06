package com.tata.omissionescalation.domain.model.events;

import java.time.Instant;

public record CaregiverAlertGenerated(
    Long omissionCaseId,
    Long intakeId,
    Long olderAdultId,
    Instant occurredAt) {
}
