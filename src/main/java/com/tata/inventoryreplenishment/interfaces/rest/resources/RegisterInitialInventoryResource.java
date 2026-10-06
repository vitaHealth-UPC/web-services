package com.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Initial stock of a medication (US-40)")
public record RegisterInitialInventoryResource(
        @Schema(description = "Logical identifier of the medication in Treatment Management",
                example = "3f1c2a8e-5b7d-4e2a-9c1f-0a6b8d4e2f10")
        @NotBlank String medicationId,
        @Schema(description = "Units available when the inventory is registered; must be positive", example = "30")
        @NotNull Integer initialQuantity,
        @Schema(description = "Stock is low when it reaches or falls below this value; cannot be negative", example = "5")
        @NotNull Integer replenishmentThreshold
) {}
