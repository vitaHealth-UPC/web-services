package com.tata.adherenceanalytics.domain.model;

import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.*;
import java.util.*;

/** Immutable evidence captured for one owner, period and calendar zone. */
public class AdherencePeriodSnapshot {
    private String id;
    private String olderAdultId;
    private Instant periodFrom;
    private Instant periodTo;
    private String calendarZone;
    private Instant consolidatedAt;
    private int onTimeIntakes;
    private int lateIntakes;
    private int omittedIntakes;
    private int minimumOmissionDays;
    private List<PatternEvidence> patterns = new ArrayList<>();
    protected AdherencePeriodSnapshot() {}
    public AdherencePeriodSnapshot(String id, String owner, Instant from, Instant to, String zone,
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
    public int confirmedIntakes() { return onTimeIntakes + lateIntakes; }
    public int totalIntakes() { return confirmedIntakes() + omittedIntakes; }
    public double percentage() { return totalIntakes() == 0 ? 0d : confirmedIntakes() * 100d / totalIntakes(); }
    public List<Pattern> patterns() { return patterns.stream().map(PatternEvidence::toPattern).toList(); }
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
