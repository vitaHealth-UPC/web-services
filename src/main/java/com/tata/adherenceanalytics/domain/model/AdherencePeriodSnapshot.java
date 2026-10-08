package com.tata.adherenceanalytics.domain.model;

import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import static com.tata.shared.domain.validation.DomainText.requireText;

/** Immutable evidence captured for one owner, period and calendar zone. */
public final class AdherencePeriodSnapshot {
    private final String id;
    private final String olderAdultId;
    private final Instant periodFrom;
    private final Instant periodTo;
    private final String calendarZone;
    private final Instant consolidatedAt;
    private final int onTimeIntakes;
    private final int lateIntakes;
    private final int omittedIntakes;
    private final int minimumOmissionDays;
    private final List<Pattern> patterns;
    /**
     * Captures metrics for a nonempty period using one calendar zone.
     * @throws IllegalArgumentException when identity, period, counts or recurrence threshold are invalid
     */
    public AdherencePeriodSnapshot(String id, String owner, Instant from, Instant to, String zone,
            int onTime, int late, int omitted, List<Pattern> patterns, int minimumOmissionDays, Instant consolidatedAt) {
        this.id = requireText(id, "id");
        this.olderAdultId = requireText(owner, "olderAdultId");
        this.periodFrom = Objects.requireNonNull(from, "from");
        this.periodTo = Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) throw new IllegalArgumentException("period must end after its start");
        this.calendarZone = ZoneId.of(requireText(zone, "calendarZone")).getId();
        if (onTime < 0 || late < 0 || omitted < 0 || (long) onTime + late + omitted > Integer.MAX_VALUE)
            throw new IllegalArgumentException("intake counts must be nonnegative and fit in an integer");
        if (minimumOmissionDays < 2) throw new IllegalArgumentException("recurrence requires at least two days");
        this.onTimeIntakes = onTime;
        this.lateIntakes = late;
        this.omittedIntakes = omitted;
        this.consolidatedAt = Objects.requireNonNull(consolidatedAt, "consolidatedAt");
        this.minimumOmissionDays = minimumOmissionDays;
        this.patterns = List.copyOf(patterns);
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
    public List<Pattern> patterns() { return patterns; }
}
