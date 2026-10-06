package com.tata.treatmentmanagement.application.models;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.LocalTime;

public record TreatmentResult(
        String id,
        String olderAdultId,
        String name,
        TreatmentStatus status,
        String medicationId,
        String dose,
        String frequency,
        LocalTime scheduledTime,
        String instructions,
        Integer reminderLeadMinutes
) {}
