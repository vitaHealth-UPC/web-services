package com.tata.intakeexecution.application.models;

import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;

import java.time.Instant;

public record IntakeResult(
        String id,
        String treatmentId,
        String medicationId,
        String olderAdultId,
        String medicationName,
        String dose,
        String instructions,
        Instant scheduledAt,
        IntakeStatus status
) {}
