package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record EscalateOmissionCommand(Long omissionCaseId, Instant now) {
}
