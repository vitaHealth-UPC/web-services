package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Exposes medication identity, display labels and availability. */
@Schema(description = "Medication of an older adult")
public record MedicationResponse(
    @Schema(example = "7a1b2c3d-1111-4222-8333-444455556666") String id,
    String olderAdultId,
    @Schema(example = "Losartán") String name,
    @Schema(example = "50 mg, tableta") String presentation,
    @Schema(
            description = "false once the medication was deactivated; its history is kept",
            example = "true")
        boolean active) {}
