package com.tata.familymonitoring.domain.model.valueobjects;

import java.time.Instant;

/** Result of a past intake, as shown in the recent history. */
public record IntakeSummary(
    String intakeId,
    String medicationName,
    Instant scheduledAt,
    IntakeStatus status) {
}
