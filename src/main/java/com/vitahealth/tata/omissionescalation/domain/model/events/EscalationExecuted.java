package com.vitahealth.tata.omissionescalation.domain.model.events;

import java.time.Instant;

/** Records one newly applied attention level with its original source occurrence time. */
public record EscalationExecuted(
    Long omissionCaseId, String intakeId, String olderAdultId, int level, Instant occurredAt)
    implements OmissionEvent {}
