package com.tata.omissionescalation.domain.model.commands;

import java.time.Instant;

public record CloseOmissionCaseCommand(Long omissionCaseId, Instant now) {
}
