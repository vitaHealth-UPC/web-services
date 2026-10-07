package com.tata.adherenceanalytics.application.internal.commandservices;

import com.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ConsolidateWeeklyPeriodCommandHandler implements com.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService {
    private final AdherenceQueryService queries;
    private final AdherenceSnapshotRepository snapshots;
    private final ApplicationEventPublisher events;
    public ConsolidateWeeklyPeriodCommandHandler(AdherenceQueryService queries, AdherenceSnapshotRepository snapshots,
            ApplicationEventPublisher events) {
        this.queries = queries; this.snapshots = snapshots; this.events = events;
    }
    public AdherencePeriodSnapshot handle(String owner, Instant from, Instant to, String zone) {
        var analysis = queries.capture(owner, from, to, zone);
        var metrics = analysis.metrics();
        final String calendarZone;
        try { calendarZone = ZoneId.of(zone).normalized().getId(); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("invalid calendar zone", exception); }
        var key = UUID.nameUUIDFromBytes((metrics.olderAdultId() + "|" + from + "|" + to + "|" + calendarZone)
                .getBytes(StandardCharsets.UTF_8)).toString();
        var existing = snapshots.find(key);
        if (existing.isPresent()) return existing.get();
        var saved = snapshots.saveIfAbsent(new AdherencePeriodSnapshot(key, metrics.olderAdultId(), from, to, calendarZone,
                metrics.onTimeIntakes(), metrics.lateIntakes(), metrics.omittedIntakes(), analysis.patterns(),
                analysis.minimumOmissionDays(),
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
        // only a new consolidation announces its patterns; a retry returns the stored one silently
        saved.patterns().forEach(pattern -> events.publishEvent(new AdherencePatternDetected(
                saved.olderAdultId(), pattern.medicationId(), pattern.omissionDays(),
                pattern.firstDay(), pattern.lastDay(), saved.consolidatedAt())));
        return saved;
    }
    public java.util.Optional<AdherencePeriodSnapshot> find(String owner, String id) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("olderAdultId is required");
        return snapshots.find(id).filter(snapshot -> snapshot.olderAdultId().equals(owner.trim()));
    }
}
