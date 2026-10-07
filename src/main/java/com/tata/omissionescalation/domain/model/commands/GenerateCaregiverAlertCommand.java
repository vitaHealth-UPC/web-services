package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record GenerateCaregiverAlertCommand(Long omissionCaseId, Instant now) {
}
