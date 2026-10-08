package com.tata.treatmentmanagement.application.models;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.LocalTime;
import java.util.List;

public record TreatmentResult(
        String id,
        String olderAdultId,
        String name,
        TreatmentStatus status,
        String medicationId,
        String dose,
        String frequency,
        List<LocalTime> scheduledTimes,
        String instructions,
        Integer reminderLeadMinutes
) {}
