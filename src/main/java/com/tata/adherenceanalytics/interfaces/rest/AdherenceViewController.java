package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.adherenceanalytics.interfaces.rest.transform.AdherenceViewResourceAssembler;
import com.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceViewResources.InsightsResource;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceViewResources.SummaryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Adherence Analytics", description = "Adherence summary and follow-up recommendations of an older adult")
@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/adherence")
public class AdherenceViewController {
    private final AdherenceViewQueryService views;

    public AdherenceViewController(AdherenceViewQueryService views) {
        this.views = views;
    }

    @Operation(summary = "Adherence summary of the last days",
            description = "Adherence and on-time percentages with the change against the previous period, trend, "
                    + "recent intakes and the concentration pattern. Pending intakes are excluded.")
    @ApiResponse(responseCode = "200", description = "Summary available")
    @ApiResponse(responseCode = "204", description = "No definitive intakes in the period")
    @ApiResponse(responseCode = "400", description = "Invalid period or calendar zone")
    @GetMapping("/summary")
    public ResponseEntity<SummaryResource> summary(
            @Parameter(description = "Older adult identifier") @PathVariable String olderAdultId,
            @Parameter(description = "Period length in days (1 to 31)") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "IANA calendar zone used to bucket the days")
            @RequestParam(defaultValue = "America/Lima") String zone) {
        return views.summary(olderAdultId, days, zoneOf(zone), Instant.now())
                .map(AdherenceViewResourceAssembler::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Follow-up recommendations from recurring patterns",
            description = "Recommendations only concern reminders, schedules and follow-up; they never change "
                    + "a dose or a medical indication.")
    @ApiResponse(responseCode = "200", description = "Pattern with enough evidence")
    @ApiResponse(responseCode = "204", description = "Insufficient evidence for a conclusive recommendation")
    @ApiResponse(responseCode = "400", description = "Invalid period or calendar zone")
    @GetMapping({"/insight", "/insights"})
    public ResponseEntity<InsightsResource> insights(
            @Parameter(description = "Older adult identifier") @PathVariable String olderAdultId,
            @Parameter(description = "Period length in days (1 to 31)") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "IANA calendar zone used to bucket the days")
            @RequestParam(defaultValue = "America/Lima") String zone) {
        return views.insights(olderAdultId, days, zoneOf(zone), Instant.now())
                .map(AdherenceViewResourceAssembler::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private static ZoneId zoneOf(String zone) {
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("invalid calendar zone", exception);
        }
    }
}
