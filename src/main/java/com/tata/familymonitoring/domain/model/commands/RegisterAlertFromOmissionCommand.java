package com.tata.familymonitoring.domain.model.commands;

import java.time.Instant;

public record RegisterAlertFromOmissionCommand(
    String olderAdultId,
    String intakeId,
    String medicationName,
    Instant scheduledAt,
    String reason) {
}
