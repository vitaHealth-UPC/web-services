package com.tata.treatmentmanagement.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public final class MedicationResources {
    private MedicationResources() {}

    public record RegisterMedicationRequest(
            @NotBlank String caregiverId,
            @NotBlank String name,
            @NotBlank String presentation
    ) {}
    public record UpdateMedicationRequest(@NotBlank String name, @NotBlank String presentation) {}
    public record MedicationResponse(
            String id, String olderAdultId, String name, String presentation, boolean active
    ) {}
}
