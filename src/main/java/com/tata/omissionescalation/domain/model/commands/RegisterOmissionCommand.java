package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record RegisterOmissionCommand(Long omissionCaseId, Instant now) {
}
