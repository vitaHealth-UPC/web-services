package com.tata.familymonitoring.domain.model.valueobjects;

import java.time.Instant;

/** Recent state of the older adult: next intake, last result and whether an alert is open. */
public record OlderAdultStatus(
    Instant nextIntakeAt,
    IntakeStatus lastIntakeStatus,
    boolean hasOpenAlert) {
}
