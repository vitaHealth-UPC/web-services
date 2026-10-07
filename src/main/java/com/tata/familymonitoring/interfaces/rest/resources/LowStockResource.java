package com.tata.familymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Medication whose stock is under its replenishment threshold")
public record LowStockResource(
    @Schema(example = "7a1b2c3d-1111-4222-8333-444455556666") String medicationId,
    @Schema(example = "Losartán") String medicationName,
    @Schema(example = "4") int remainingStock,
    @Schema(example = "5") int replenishmentThreshold,
    @Schema(example = "2026-10-06T12:00:00Z") Instant detectedAt) {
}
