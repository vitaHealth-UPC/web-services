package com.tata.adherenceanalytics.infrastructure.persistence.jpa.entities;

import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Immutable evidence captured for one owner, period and calendar zone. */
@Entity
@Table(name = "adherence_period_snapshots")
public class AdherenceSnapshotPersistenceEntity {
    @Id private String id;
    @Column(nullable = false) private String olderAdultId;
    @Column(nullable = false) private Instant periodFrom;
    @Column(nullable = false) private Instant periodTo;
    @Column(nullable = false) private String calendarZone;
    @Column(nullable = false) private Instant consolidatedAt;
    private int onTimeIntakes;
    private int lateIntakes;
    private int omittedIntakes;
    private int minimumOmissionDays;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "adherence_snapshot_patterns", joinColumns = @JoinColumn(name = "snapshot_id"))
    @OrderColumn(name = "pattern_position")
    private List<PatternEvidence> patterns = new ArrayList<>();
    protected AdherenceSnapshotPersistenceEntity() {}
    public AdherenceSnapshotPersistenceEntity(String id, String owner, Instant from, Instant to, String zone,
            int onTime, int late, int omitted, List<Pattern> patterns, int minimumOmissionDays, Instant consolidatedAt) {
        this.id = id; this.olderAdultId = owner; this.periodFrom = from; this.periodTo = to;
        this.calendarZone = zone; this.onTimeIntakes = onTime; this.lateIntakes = late;
        this.omittedIntakes = omitted; this.consolidatedAt = consolidatedAt;
        this.minimumOmissionDays = minimumOmissionDays;
        this.patterns = new ArrayList<>(patterns.stream().map(PatternEvidence::new).toList());
    }
    public String id() { return id; }
    public String olderAdultId() { return olderAdultId; }
    public Instant from() { return periodFrom; }
    public Instant to() { return periodTo; }
    public String zone() { return calendarZone; }
    public Instant consolidatedAt() { return consolidatedAt; }
    public int onTimeIntakes() { return onTimeIntakes; }
    public int lateIntakes() { return lateIntakes; }
    public int omittedIntakes() { return omittedIntakes; }
    public int minimumOmissionDays() { return minimumOmissionDays; }
    public List<Pattern> patterns() { return patterns.stream().map(PatternEvidence::toPattern).toList(); }
    @Embeddable
    public static class PatternEvidence {
        private String medicationId;
        private int omissionDays;
        private LocalDate firstDay;
        private LocalDate lastDay;
        protected PatternEvidence() {}
        PatternEvidence(Pattern pattern) {
            medicationId = pattern.medicationId(); omissionDays = pattern.omissionDays();
            firstDay = pattern.firstDay(); lastDay = pattern.lastDay();
        }
        Pattern toPattern() { return new Pattern(medicationId, omissionDays, firstDay, lastDay); }
    }
}
