package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Result of a past intake")
public record IntakeSummaryResource(
    @Schema(example = "101") String intakeId,
    @Schema(example = "Losartan 50 mg") String medicationName,
    @Schema(example = "2026-10-05T13:00:00Z") Instant scheduledAt,
    @Schema(example = "CONFIRMED") IntakeStatus status) {
}
