package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record OpenOmissionCaseCommand(
    String intakeId,
    String olderAdultId,
    String medicationName,
    Instant scheduledAt) {
}
