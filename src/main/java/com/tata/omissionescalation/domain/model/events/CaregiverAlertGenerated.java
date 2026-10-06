package com.tata.omissionescalation.domain.model.events;

import java.time.Instant;

public record CaregiverAlertGenerated(
    Long omissionCaseId,
    String intakeId,
    String olderAdultId,
    Instant occurredAt) {
}
