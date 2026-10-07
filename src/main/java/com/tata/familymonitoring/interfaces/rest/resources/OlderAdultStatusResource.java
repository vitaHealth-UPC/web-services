package com.tata.familymonitoring.interfaces.rest.resources;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Recent state of the older adult. Fields are null when there is no data yet")
public record OlderAdultStatusResource(
    @Schema(example = "2026-10-05T21:00:00Z", nullable = true) Instant nextIntakeAt,
    @Schema(example = "CONFIRMED", nullable = true) IntakeStatus lastIntakeStatus,
    @Schema(example = "true") boolean hasOpenAlert,
    List<AlertSummaryResource> openAlerts,
    AdherenceSnapshot weeklyAdherence,
    @Schema(description = "Medications running out; empty when the stock is fine") List<LowStockResource> lowStock,
    @Schema(description = "Latest adherence patterns, most recent first") List<AdherenceInsightResource> adherenceInsights) {
}
