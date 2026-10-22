package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Captures caregiver-owned medication registration without persistence concerns. */
@Schema(description = "Request to register a medication for an older adult")
public record RegisterMedicationRequest(
    @Schema(
            description = "Caregiver with an active care link to the older adult",
            example = "00000000-0000-0000-0000-000000000001")
        @NotBlank
        String caregiverId,
    @Schema(example = "Losartán") @NotBlank @Size(max = 160) String name,
    @Schema(example = "50 mg, tableta") @NotBlank @Size(max = 160) String presentation) {}
