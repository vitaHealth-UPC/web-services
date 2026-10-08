package com.tata.adherenceanalytics.interfaces.rest;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;

import com.tata.adherenceanalytics.interfaces.rest.transform.AdherenceSnapshotResourceAssembler;
import com.tata.adherenceanalytics.interfaces.rest.resources.WeeklyAdherenceResource;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceSnapshotResource;
import com.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import com.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/adherence")
public class AdherenceController {
    private final AdherenceQueryService queries;
    private final com.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService consolidations;
    public AdherenceController(AdherenceQueryService queries,
            com.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService consolidations) {
        this.queries = queries; this.consolidations = consolidations;
    }
    @PostMapping("/consolidations")
    public AdherenceSnapshotResource consolidate(@PathVariable String olderAdultId, @RequestParam Instant from,
            @RequestParam Instant to, @RequestParam(defaultValue = "UTC") String zone) {
        return AdherenceSnapshotResourceAssembler.from(consolidations.handle(olderAdultId, from, to, zone));
    }
    @GetMapping("/consolidations/{snapshotId}")
    public AdherenceSnapshotResource snapshot(@PathVariable String olderAdultId, @PathVariable String snapshotId) {
        return consolidations.find(olderAdultId, snapshotId).map(AdherenceSnapshotResourceAssembler::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }
    @GetMapping("/weekly")
    public WeeklyAdherenceResource weekly(@PathVariable String olderAdultId, @RequestParam Instant from, @RequestParam Instant to) {
        var metrics = queries.weekly(olderAdultId, from, to);
        return new WeeklyAdherenceResource(metrics.olderAdultId(), from, to, metrics.confirmedIntakes(),
                metrics.totalIntakes(), metrics.percentage(), metrics.onTimeIntakes(), metrics.lateIntakes(), metrics.omittedIntakes());
    }
    @GetMapping("/patterns")
    public List<AdherencePatternDetectionService.Pattern> patterns(@PathVariable String olderAdultId, @RequestParam Instant from,
            @RequestParam Instant to, @RequestParam(defaultValue = "UTC") String zone) {
        return queries.patterns(olderAdultId, from, to, zone);
    }
    @GetMapping("/history")
    public List<IntakeOutcome> history(@PathVariable String olderAdultId,
            @RequestParam Instant from, @RequestParam Instant to) {
        return queries.history(olderAdultId, from, to);
    }
    @GetMapping("/recommendations")
    public List<AdherenceInsightGenerationService.Insight> recommendations(@PathVariable String olderAdultId,
            @RequestParam Instant from, @RequestParam Instant to, @RequestParam(defaultValue = "UTC") String zone) {
        return queries.recommendations(olderAdultId, from, to, zone);
    }
}
