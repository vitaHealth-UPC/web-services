package com.tata.intakeexecution.domain.model.commands;

import java.time.Instant;

public record ReportUnconfirmedIntakesCommand(Instant cutoff, Instant reportedAt) {
}
