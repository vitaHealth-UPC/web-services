package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import java.time.Instant;

/**
 * Recent adult evidence and pending follow-up.
 *
 * @param hasOpenAlert whether an alert awaits explicit closure, including attended alerts
 */
public record OlderAdultStatus(
    Instant nextIntakeAt, IntakeStatus lastIntakeStatus, boolean hasOpenAlert) {}
