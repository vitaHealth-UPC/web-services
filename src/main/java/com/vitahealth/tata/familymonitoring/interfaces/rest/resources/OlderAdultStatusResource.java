package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Represents recent adult evidence and alerts awaiting explicit caregiver closure. */
@Schema(
    description =
        "Recent state of the older adult. Missing intake evidence is null; evidence lists are empty"
            + " when absent")
public record OlderAdultStatusResource(
    @Schema(example = "2026-10-05T21:00:00Z", nullable = true) Instant nextIntakeAt,
    @Schema(example = "CONFIRMED", nullable = true) IntakeStatus lastIntakeStatus,
    @Schema(description = "An OPEN or ATTENDED alert awaits explicit closure", example = "true")
        boolean hasOpenAlert,
    @Schema(description = "Unclosed alerts, including those already attended")
        List<AlertSummaryResource> openAlerts,
    AdherenceSnapshot weeklyAdherence,
    @Schema(description = "Medications running out; empty when the stock is fine")
        List<LowStockResource> lowStock,
    @Schema(description = "Latest adherence patterns, most recent first")
        List<AdherenceInsightResource> adherenceInsights) {}
