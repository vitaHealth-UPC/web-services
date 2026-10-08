package com.tata.omissionescalation.domain.model.events;

import java.time.Instant;

/** Consumed by Adherence Analytics and Family Monitoring. */
public record IntakeOmitted(
    String intakeId,
    String olderAdultId,
    String medicationName,
    Instant scheduledAt,
    String reason,
    Instant occurredAt) {
}
