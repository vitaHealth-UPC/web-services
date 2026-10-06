package com.tata.treatmentmanagement.domain.model.valueobjects;

import java.time.LocalTime;
import java.util.Objects;

public record TreatmentRegimen(
        String medicationId,
        String dose,
        String frequency,
        LocalTime scheduledTime,
        String instructions,
        int reminderLeadMinutes
) {
    public TreatmentRegimen {
        medicationId = requireText(medicationId, "medicationId");
        dose = requireText(dose, "dose");
        frequency = requireText(frequency, "frequency");
        Objects.requireNonNull(scheduledTime, "scheduledTime is required");
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
