package com.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public final class TreatmentResources {
    private TreatmentResources() {}

    @Schema(description = "Request to create a treatment in DRAFT status")
    public record CreateTreatmentRequest(
            @Schema(description = "Caregiver with an active care link to the older adult", example = "00000000-0000-0000-0000-000000000001")
            @NotBlank String caregiverId,
            @Schema(example = "Control de presión")
            @NotBlank String name
    ) {}

    @Schema(description = "Dose, frequency, schedule, instructions and reminder of the treatment")
    public record ConfigureTreatmentRequest(
            @Schema(example = "00000000-0000-0000-0000-000000000001")
            @NotBlank String caregiverId,
            @Schema(description = "Active medication of the same older adult")
            @NotBlank String medicationId,
            @Schema(example = "1 comprimido")
            @NotBlank String dose,
            @Schema(example = "DAILY")
            @NotBlank String frequency,
            @Schema(example = "[\"08:00:00\", \"20:00:00\"]")
            @NotEmpty List<@NotNull LocalTime> scheduledTimes,
            @Schema(example = "Con un vaso de agua")
            String instructions,
            @Schema(description = "Minutes before each intake when the reminder is sent", example = "10")
            @Min(0) @Max(1440) int reminderLeadMinutes
    ) {}

    @Schema(description = "Treatment with its current regimen. Regimen fields are null while the treatment is incomplete")
    public record TreatmentResponse(
            @Schema(example = "3f6c1d0e-8a52-4f0b-9c55-2f1f4d9a7b10") String id,
            String olderAdultId,
            @Schema(example = "Control de presión") String name,
            @Schema(description = "DRAFT, ACTIVE or PAUSED", example = "DRAFT") String status,
            String medicationId,
            @Schema(example = "1 comprimido") String dose,
            @Schema(example = "DAILY") String frequency,
            @Schema(example = "[\"08:00:00\", \"20:00:00\"]") List<LocalTime> scheduledTimes,
            @Schema(example = "Con un vaso de agua") String instructions,
            @Schema(example = "10") Integer reminderLeadMinutes
    ) {}
}
