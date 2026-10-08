package com.tata.familymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Repeated omissions of a medication found by the weekly adherence analysis")
public record AdherenceInsightResource(
    @Schema(example = "7a1b2c3d-1111-4222-8333-444455556666") String medicationId,
    @Schema(example = "Losartán") String medicationName,
    @Schema(example = "3") int omissionDays,
    @Schema(example = "2026-10-01") LocalDate firstDay,
    @Schema(example = "2026-10-03") LocalDate lastDay,
    @Schema(example = "2026-10-06T08:00:00Z") Instant detectedAt) {
}
