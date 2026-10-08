package com.tata.adherenceanalytics.application.queryservices;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;

import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import com.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import java.time.*;
import java.util.List;

public interface AdherenceQueryService {
    public record Metrics(String olderAdultId, Instant from, Instant to, int confirmedIntakes,
                          int totalIntakes, double percentage, int onTimeIntakes, int lateIntakes, int omittedIntakes) {}
    public record Analysis(Metrics metrics, List<AdherencePatternDetectionService.Pattern> patterns, int minimumOmissionDays) {}
    Metrics weekly(String owner, Instant from, Instant to);
    List<IntakeOutcome> history(String owner, Instant from, Instant to);
    List<AdherencePatternDetectionService.Pattern> patterns(String owner, Instant from, Instant to, String zone);
    Analysis capture(String owner, Instant from, Instant to, String zone);
    List<AdherenceInsightGenerationService.Insight> recommendations(String owner, Instant from, Instant to, String zone);
}
