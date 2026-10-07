package com.tata.treatmentmanagement.domain.model.valueobjects;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public record TreatmentRegimen(
        String medicationId,
        String dose,
        String frequency,
        List<LocalTime> scheduledTimes,
        String instructions,
        int reminderLeadMinutes
) {
    public TreatmentRegimen {
        medicationId = requireText(medicationId, "medicationId");
        dose = requireText(dose, "dose");
        frequency = requireText(frequency, "frequency");
        if (scheduledTimes == null || scheduledTimes.isEmpty()) {
            throw new IllegalArgumentException("at least one scheduled time is required");
        }
        scheduledTimes = List.copyOf(
                scheduledTimes.stream()
                        .map(time -> Objects.requireNonNull(time, "scheduled time is required"))
                        .distinct()
                        .sorted()
                        .toList()
        );
        instructions = instructions == null ? "" : instructions.trim();
        if (reminderLeadMinutes < 0 || reminderLeadMinutes > 24 * 60) {
            throw new IllegalArgumentException("reminderLeadMinutes must be between 0 and 1440");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
