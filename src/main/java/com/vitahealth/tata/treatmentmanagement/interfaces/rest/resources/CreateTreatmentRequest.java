package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Captures the name and caregiver scope of a new draft treatment. */
@Schema(description = "Request to create a treatment in DRAFT status")
public record CreateTreatmentRequest(
    @Schema(
            description = "Caregiver with an active care link to the older adult",
            example = "00000000-0000-0000-0000-000000000001")
        @NotBlank
        String caregiverId,
    @Schema(example = "Control de presión") @NotBlank @Size(max = 160) String name) {}
