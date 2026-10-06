package com.tata.intakeexecution.application.commands;

import java.time.Instant;

public record ReportUnconfirmedIntakesCommand(Instant cutoff, Instant reportedAt) {
}