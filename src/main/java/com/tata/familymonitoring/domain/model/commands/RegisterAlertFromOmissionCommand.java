package com.tata.familymonitoring.domain.model.commands;

import java.time.Instant;

public record RegisterAlertFromOmissionCommand(
    Long olderAdultId,
    Long intakeId,
    String medicationName,
    Instant scheduledAt,
    String reason) {
}
