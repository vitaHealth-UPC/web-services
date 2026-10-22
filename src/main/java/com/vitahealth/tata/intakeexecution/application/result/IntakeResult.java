package com.vitahealth.tata.intakeexecution.application.result;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import java.time.Instant;

/** Immutable intake evidence projection; alreadyConfirmed describes only command replay. */
public record IntakeResult(
    String id,
    String treatmentId,
    String medicationId,
    String olderAdultId,
    String medicationName,
    String dose,
    String instructions,
    Instant scheduledAt,
    IntakeStatus status,
    Instant confirmedAt,
    ConfirmationChannel confirmationChannel,
    boolean alreadyConfirmed) {}
