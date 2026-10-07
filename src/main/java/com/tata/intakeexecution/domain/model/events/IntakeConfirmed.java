package com.tata.intakeexecution.domain.model.events;

import java.time.Instant;

public record IntakeConfirmed(String intakeId, String medicationId, String olderAdultId, Instant confirmedAt) {}
