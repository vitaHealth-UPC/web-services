package com.tata.adherenceanalytics.application.internal.queryservices;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;

import com.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;

import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.tata.adherenceanalytics.application.models.IntakeOutcome.Status;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import com.tata.adherenceanalytics.domain.services.AdherenceInsightGenerationService;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdherenceQueryServiceImpl implements AdherenceQueryService {
    private final IntakeOutcomePort outcomes;
    private final int minimumOmissionDays;
    private final AdherencePatternDetectionService detector = new AdherencePatternDetectionService();

    public AdherenceQueryServiceImpl(IntakeOutcomePort outcomes,
            @Value("${adherence.pattern.minimum-omission-days:3}") int minimumOmissionDays) {
        if (minimumOmissionDays < 2 || minimumOmissionDays > 8)
            throw new IllegalArgumentException("minimum omission days must be between two and eight");
        this.outcomes = outcomes;
        this.minimumOmissionDays = minimumOmissionDays;
    }
    public Metrics weekly(String owner, Instant from, Instant to) {
        var evidence = history(owner, from, to);
        return metrics(owner, from, to, evidence);
    }
    private Metrics metrics(String owner, Instant from, Instant to, List<IntakeOutcome> evidence) {
        int confirmed = (int) evidence.stream().filter(i -> i.status() == Status.CONFIRMED || i.status() == Status.LATE).count();
        return new Metrics(owner.trim(), from, to, confirmed, evidence.size(),
                evidence.isEmpty() ? 0d : confirmed * 100d / evidence.size(),
                (int) evidence.stream().filter(i -> i.status() == Status.CONFIRMED).count(),
                (int) evidence.stream().filter(i -> i.status() == Status.LATE).count(),
                (int) evidence.stream().filter(i -> i.status() == Status.OMITTED).count());
    }
    public List<IntakeOutcome> history(String owner, Instant from, Instant to) {
        validateRange(owner, from, to);
        return outcomes.find(owner.trim(), from, to).stream().filter(i -> i.status() != Status.PENDING).toList();
    }
    public List<AdherencePatternDetectionService.Pattern> patterns(String owner, Instant from, Instant to, String zone) {
        return detect(history(owner, from, to), zone);
    }
    private List<AdherencePatternDetectionService.Pattern> detect(List<IntakeOutcome> evidence, String zone) {
        ZoneId calendarZone;
        try { calendarZone = ZoneId.of(zone); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("invalid calendar zone", exception); }
        return detector.detect(evidence.stream().map(i ->
                new AdherencePatternDetectionService.Outcome(i.medicationId(), i.scheduledAt(), i.status() == Status.OMITTED))
                .toList(), calendarZone, minimumOmissionDays);
    }

    public Analysis capture(String owner, Instant from, Instant to, String zone) {
        var evidence = history(owner, from, to);
        return new Analysis(metrics(owner, from, to, evidence), detect(evidence, zone), minimumOmissionDays);
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
