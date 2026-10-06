package com.tata.adherenceanalytics.application;

import com.tata.adherenceanalytics.application.ports.IntakeOutcomePort;
import com.tata.adherenceanalytics.application.ports.IntakeOutcomePort.Status;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import com.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdherenceQueryService {
    private final IntakeOutcomePort outcomes;
    private final AdherencePatternDetectionService detector = new AdherencePatternDetectionService();
    public record Metrics(String olderAdultId, Instant from, Instant to, int confirmedIntakes,
                          int totalIntakes, double percentage) {}
    public AdherenceQueryService(IntakeOutcomePort outcomes) { this.outcomes = outcomes; }
    public Metrics weekly(String owner, Instant from, Instant to) {
        var evidence = history(owner, from, to);
        int confirmed = (int) evidence.stream().filter(i -> i.status() == Status.CONFIRMED || i.status() == Status.LATE).count();
        return new Metrics(owner.trim(), from, to, confirmed, evidence.size(),
                evidence.isEmpty() ? 0d : confirmed * 100d / evidence.size());
    }
    public List<IntakeOutcomePort.Outcome> history(String owner, Instant from, Instant to) {
        validateRange(owner, from, to);
        return outcomes.find(owner.trim(), from, to).stream().filter(i -> i.status() != Status.PENDING).toList();
    }
    public List<AdherencePatternDetectionService.Pattern> patterns(String owner, Instant from, Instant to, String zone) {
        ZoneId calendarZone;
        try { calendarZone = ZoneId.of(zone); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("invalid calendar zone", exception); }
        return detector.detect(history(owner, from, to).stream().map(i ->
                new AdherencePatternDetectionService.Outcome(i.medicationId(), i.scheduledAt(), i.status() == Status.OMITTED))
                .toList(), calendarZone, 3);
    }
    public List<AdherenceInsightGenerationService.Insight> recommendations(String owner, Instant from, Instant to, String zone) {
        return new AdherenceInsightGenerationService().generate(patterns(owner, from, to, zone));
    }
    private static void validateRange(String owner, Instant from, Instant to) {
        if (owner == null || owner.isBlank() || from == null || to == null || !to.isAfter(from)
                || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0)
            throw new IllegalArgumentException("adherence range must be ordered and at most eight days");
    }
}
