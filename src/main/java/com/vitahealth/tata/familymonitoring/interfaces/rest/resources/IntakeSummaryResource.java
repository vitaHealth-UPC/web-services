package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Represents an observed past intake; pending future doses are excluded from history. */
@Schema(description = "Result of a past intake")
public record IntakeSummaryResource(
    @Schema(example = "7a1b2c3d-1111-4222-8333-444455556666") String intakeId,
    @Schema(example = "Losartan 50 mg") String medicationName,
    @Schema(example = "2026-10-05T13:00:00Z") Instant scheduledAt,
    @Schema(example = "CONFIRMED") IntakeStatus status) {}
