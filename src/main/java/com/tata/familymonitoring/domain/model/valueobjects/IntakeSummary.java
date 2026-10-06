package com.tata.familymonitoring.domain.model.valueobjects;

import java.time.Instant;

/** Result of a past intake, as shown in the recent history. */
public record IntakeSummary(
    Long intakeId,
    String medicationName,
    Instant scheduledAt,
    IntakeStatus status) {
}
