package com.vitahealth.tata.treatmentmanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Captures editable medication labels while preserving identity and history. */
@Schema(description = "Request to edit the name or presentation of an active medication")
public record UpdateMedicationRequest(
    @Schema(example = "00000000-0000-0000-0000-000000000001") @NotBlank String caregiverId,
    @Schema(example = "Losartán") @NotBlank @Size(max = 160) String name,
    @Schema(example = "100 mg, tableta") @NotBlank @Size(max = 160) String presentation) {}
