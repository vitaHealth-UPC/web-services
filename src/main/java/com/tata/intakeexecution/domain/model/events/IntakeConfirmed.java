package com.tata.intakeexecution.domain.model.events;

import java.time.Instant;

/** Published by Intake Execution when an intake is confirmed by tap or by voice. */
public record IntakeConfirmed(Long intakeId, Instant confirmedAt) {
}
