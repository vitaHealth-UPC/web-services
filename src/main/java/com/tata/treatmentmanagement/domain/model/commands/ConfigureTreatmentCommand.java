package com.tata.treatmentmanagement.domain.model.commands;

import java.time.LocalTime;

public record ConfigureTreatmentCommand(
        String treatmentId,
        String medicationId,
        String dose,
        String frequency,
        LocalTime scheduledTime,
        String instructions,
        int reminderLeadMinutes
) {}
