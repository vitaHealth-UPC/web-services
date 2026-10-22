package com.vitahealth.tata.omissionescalation.domain.model.events;

import java.time.Instant;

/** Records creation of one business alert independently of later device delivery attempts. */
public record CaregiverAlertGenerated(
    Long omissionCaseId, String intakeId, String olderAdultId, Instant occurredAt)
    implements OmissionEvent {}
