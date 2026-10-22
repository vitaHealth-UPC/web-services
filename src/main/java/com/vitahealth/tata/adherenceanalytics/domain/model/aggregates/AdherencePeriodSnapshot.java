package com.vitahealth.tata.adherenceanalytics.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
  private final List<AdherenceOmissionPattern> patterns;

  /**
   * Captures metrics for a nonempty period using one calendar zone.
   *
   * @throws IllegalArgumentException when identity, period, counts or recurrence threshold are
   *     invalid
   */
  public AdherencePeriodSnapshot(
      String id,
      String owner,
      Instant from,
      Instant to,
      String zone,
      int onTime,
      int late,
      int omitted,
      List<AdherenceOmissionPattern> patterns,
      int minimumOmissionDays,
      Instant consolidatedAt) {
    this.id = requireText(id, "id");
    this.olderAdultId = requireText(owner, "olderAdultId");
    this.periodFrom = Objects.requireNonNull(from, "from");
    this.periodTo = Objects.requireNonNull(to, "to");
    if (!from.isBefore(to)) throw new IllegalArgumentException("period must end after its start");
    this.calendarZone = ZoneId.of(requireText(zone, "calendarZone")).getId();
    if (onTime < 0 || late < 0 || omitted < 0 || (long) onTime + late + omitted > Integer.MAX_VALUE)
      throw new IllegalArgumentException("intake counts must be nonnegative and fit in an integer");
    if (minimumOmissionDays < 2)
      throw new IllegalArgumentException("recurrence requires at least two days");
    this.onTimeIntakes = onTime;
    this.lateIntakes = late;
    this.omittedIntakes = omitted;
    this.consolidatedAt = Objects.requireNonNull(consolidatedAt, "consolidatedAt");
    this.minimumOmissionDays = minimumOmissionDays;
    var evidence = List.copyOf(patterns);
    var medications = new HashSet<String>();
    var calendar = ZoneId.of(calendarZone);
    var firstDate = from.atZone(calendar).toLocalDate();
    var lastDate = to.minusNanos(1).atZone(calendar).toLocalDate();
    long patternDays = 0;
    for (var pattern : evidence) {
      if (!medications.add(pattern.medicationId())
          || pattern.omissionDays() < minimumOmissionDays
          || pattern.firstDay().isBefore(firstDate)
          || pattern.lastDay().isAfter(lastDate))
        throw new IllegalArgumentException(
            "patterns conflict with the captured period or recurrence policy");
      patternDays = Math.addExact(patternDays, pattern.omissionDays());
    }
    if (patternDays > omitted)
      throw new IllegalArgumentException("pattern days exceed recorded omissions");
    this.patterns = evidence;
  }

  /** Returns the immutable stored period identity. */
  public String id() {
    return id;
  }

  /** Returns the logical owner of the captured evidence. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the inclusive start instant. */
  public Instant from() {
    return periodFrom;
  }

  /** Returns the exclusive end instant. */
  public Instant to() {
    return periodTo;
  }

  /** Returns the calendar zone used for recurrence detection. */
  public String zone() {
    return calendarZone;
  }

  /** Returns the original capture instant. */
  public Instant consolidatedAt() {
    return consolidatedAt;
  }

  /** Returns on-time confirmed occurrences. */
  public int onTimeIntakes() {
    return onTimeIntakes;
  }

  /** Returns late confirmed occurrences. */
  public int lateIntakes() {
    return lateIntakes;
  }

  /** Returns omitted occurrences included in the denominator. */
  public int omittedIntakes() {
    return omittedIntakes;
  }

  /** Returns the recurrence threshold used during capture. */
  public int minimumOmissionDays() {
    return minimumOmissionDays;
  }

  /** Returns both on-time and late confirmations. */
  public int confirmedIntakes() {
    return onTimeIntakes + lateIntakes;
  }

  /** Returns completed outcomes; pending observations are excluded. */
  public int totalIntakes() {
    return confirmedIntakes() + omittedIntakes;
  }

  /** Returns confirmed share of completed outcomes, or zero for no completed evidence. */
  public double percentage() {
    return totalIntakes() == 0 ? 0d : confirmedIntakes() * 100d / totalIntakes();
  }

  /** Returns immutable original recurrence evidence. */
  public List<AdherenceOmissionPattern> patterns() {
    return patterns;
  }
}
