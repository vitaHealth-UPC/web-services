package com.tata.treatmentmanagement.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public final class TreatmentResources {
    private TreatmentResources() {}

    public record CreateTreatmentRequest(@NotBlank String caregiverId, @NotBlank String name) {}

    public record ConfigureTreatmentRequest(
            @NotBlank String caregiverId,
            @NotBlank String medicationId,
            @NotBlank String dose,
            @NotBlank String frequency,
            @NotNull LocalTime scheduledTime,
            String instructions,
            @Min(0) @Max(1440) int reminderLeadMinutes
    ) {}

    public record TreatmentResponse(
            String id,
            String olderAdultId,
            String name,
            String status,
            String medicationId,
            String dose,
            String frequency,
            LocalTime scheduledTime,
            String instructions,
            Integer reminderLeadMinutes
    ) {}
}
