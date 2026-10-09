package com.tata.intakeexecution.interfaces.rest.resources;

import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;

import java.time.Instant;

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
        boolean alreadyConfirmed
) {}
