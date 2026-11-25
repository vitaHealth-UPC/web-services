package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Caregiver correction of a mistyped newest receipt. */
@Schema(description = "Corrected units of the newest receipt")
public record CorrectReceiptResource(
    @Schema(description = "Corrected units; must be positive", example = "20") @NotNull
        Integer quantity,
    @Schema(description = "Optional lot identifier or note") @Size(max = 200) String lot,
    @Schema(description = "Corrected low-stock threshold; only with the initial stock")
        Integer replenishmentThreshold) {}
