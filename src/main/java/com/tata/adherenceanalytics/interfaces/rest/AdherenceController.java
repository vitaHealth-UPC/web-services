package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.adherenceanalytics.application.AdherenceQueryService;
import com.tata.adherenceanalytics.application.ports.IntakeOutcomePort;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import com.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/adherence")
public class AdherenceController {
    private final AdherenceQueryService queries;
    public AdherenceController(AdherenceQueryService queries) { this.queries = queries; }
    @GetMapping("/weekly")
    public WeeklyAdherenceResource weekly(@PathVariable String olderAdultId, @RequestParam Instant from, @RequestParam Instant to) {
        var metrics = queries.weekly(olderAdultId, from, to);
        return new WeeklyAdherenceResource(metrics.olderAdultId(), from, to, metrics.confirmedIntakes(), metrics.totalIntakes(), metrics.percentage());
    }
    @GetMapping("/patterns")
    public List<AdherencePatternDetectionService.Pattern> patterns(@PathVariable String olderAdultId, @RequestParam Instant from,
            @RequestParam Instant to, @RequestParam(defaultValue = "UTC") String zone) {
        return queries.patterns(olderAdultId, from, to, zone);
    }
    @GetMapping("/history")
    public List<IntakeOutcomePort.Outcome> history(@PathVariable String olderAdultId,
            @RequestParam Instant from, @RequestParam Instant to) {
        return queries.history(olderAdultId, from, to);
    }
    @GetMapping("/recommendations")
    public List<AdherenceInsightGenerationService.Insight> recommendations(@PathVariable String olderAdultId,
            @RequestParam Instant from, @RequestParam Instant to, @RequestParam(defaultValue = "UTC") String zone) {
        return queries.recommendations(olderAdultId, from, to, zone);
    }
}
