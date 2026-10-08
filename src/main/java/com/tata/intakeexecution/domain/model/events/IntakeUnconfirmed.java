package com.tata.intakeexecution.domain.model.events;

import java.time.Instant;

/** Published by Intake Execution when a scheduled intake stays without confirmation. */
public record IntakeUnconfirmed(
    String intakeId,
    String olderAdultId,
    String medicationName,
    Instant scheduledAt) {
}
