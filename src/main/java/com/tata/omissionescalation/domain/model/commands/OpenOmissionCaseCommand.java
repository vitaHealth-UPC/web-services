package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record OpenOmissionCaseCommand(
    Long intakeId,
    Long olderAdultId,
    String medicationName,
    Instant scheduledAt) {
}
