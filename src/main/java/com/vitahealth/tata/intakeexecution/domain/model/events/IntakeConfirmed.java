package com.vitahealth.tata.intakeexecution.domain.model.events;

import java.time.Instant;

/** Original intake confirmation evidence delivered atomically to dependent contexts. */
public record IntakeConfirmed(
    String intakeId, String medicationId, String olderAdultId, Instant confirmedAt) {}
