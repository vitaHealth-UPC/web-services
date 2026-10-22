package com.vitahealth.tata.intakeexecution.interfaces.rest.resources;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import java.time.Instant;

/** Public intake evidence representation with optional confirmation metadata. */
public record IntakeResource(
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
