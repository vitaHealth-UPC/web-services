package com.tata.adherenceanalytics.application;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsolidateWeeklyPeriodCommandHandler {
    private final AdherenceQueryService queries;
    private final AdherenceSnapshotRepository snapshots;
    public ConsolidateWeeklyPeriodCommandHandler(AdherenceQueryService queries, AdherenceSnapshotRepository snapshots) {
        this.queries = queries; this.snapshots = snapshots;
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
        return snapshots.saveIfAbsent(new AdherencePeriodSnapshot(key, metrics.olderAdultId(), from, to, calendarZone,
                metrics.onTimeIntakes(), metrics.lateIntakes(), metrics.omittedIntakes(), analysis.patterns(),
                analysis.minimumOmissionDays(),
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
    }
    public java.util.Optional<AdherencePeriodSnapshot> find(String owner, String id) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("olderAdultId is required");
        return snapshots.find(id).filter(snapshot -> snapshot.olderAdultId().equals(owner.trim()));
    }
}
