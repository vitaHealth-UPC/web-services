package com.tata.treatmentmanagement.domain.model.commands;

import java.time.LocalTime;
import java.util.List;

public record ConfigureTreatmentCommand(
        String caregiverId,
        String treatmentId,
        String medicationId,
        String dose,
        String frequency,
        List<LocalTime> scheduledTimes,
        String instructions,
        int reminderLeadMinutes
) {}
