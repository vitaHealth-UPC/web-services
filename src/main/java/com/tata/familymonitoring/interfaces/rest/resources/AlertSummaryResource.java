package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Follow-up state of an alert raised by an omitted intake")
public record AlertSummaryResource(
    @Schema(example = "1") Long id,
    @Schema(example = "101") String intakeId,
    @Schema(example = "Losartan 50 mg") String medicationName,
    @Schema(example = "2026-10-05T13:00:00Z") Instant scheduledAt,
    @Schema(example = "Intake not confirmed within the grace period") String reason,
    @Schema(example = "OPEN") AlertStatus status,
    @Schema(example = "2026-10-05T13:30:00Z") Instant openedAt,
    @Schema(example = "null", nullable = true) Instant closedAt) {
}
