package com.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class MedicationResources {
    private MedicationResources() {}

    @Schema(description = "Request to register a medication for an older adult")
    public record RegisterMedicationRequest(
            @Schema(description = "Caregiver with an active care link to the older adult", example = "00000000-0000-0000-0000-000000000001")
            @NotBlank String caregiverId,
            @Schema(example = "Losartán") @NotBlank String name,
            @Schema(example = "50 mg, tableta") @NotBlank String presentation
    ) {}

    @Schema(description = "Request to edit the name or presentation of an active medication")
    public record UpdateMedicationRequest(
            @Schema(example = "00000000-0000-0000-0000-000000000001") @NotBlank String caregiverId,
            @Schema(example = "Losartán") @NotBlank String name,
            @Schema(example = "100 mg, tableta") @NotBlank String presentation
    ) {}

    @Schema(description = "Medication of an older adult")
    public record MedicationResponse(
            @Schema(example = "7a1b2c3d-1111-4222-8333-444455556666") String id,
            String olderAdultId,
            @Schema(example = "Losartán") String name,
            @Schema(example = "50 mg, tableta") String presentation,
            @Schema(description = "false once the medication was deactivated; its history is kept", example = "true")
            boolean active
    ) {}
}
