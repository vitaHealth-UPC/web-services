package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record ResolveOmissionCaseCommand(String intakeId, Instant confirmedAt) {}
