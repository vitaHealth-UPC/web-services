package com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.embeddables.AdherencePatternEmbeddable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Immutable evidence captured for one owner, period and calendar zone. */
@Entity
@Table(name = "adherence_period_snapshots")
public class AdherenceSnapshotPersistenceEntity {
  @Id private String id;

  @Column(nullable = false)
  private String olderAdultId;

  @Column(nullable = false)
  private Instant periodFrom;

  @Column(nullable = false)
  private Instant periodTo;

  @Column(nullable = false)
  private String calendarZone;

  @Column(nullable = false)
  private Instant consolidatedAt;

  private int onTimeIntakes;
  private int lateIntakes;
  private int omittedIntakes;
  private int minimumOmissionDays;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "adherence_snapshot_patterns",
      joinColumns = @JoinColumn(name = "snapshot_id"))
  @OrderColumn(name = "pattern_position")
  private List<AdherencePatternEmbeddable> patterns = new ArrayList<>();

  /** Required by JPA field access. */
  protected AdherenceSnapshotPersistenceEntity() {}

  /** Captures scalar snapshot evidence and its ordered owned pattern values. */
  public AdherenceSnapshotPersistenceEntity(
      String id,
      String owner,
      Instant from,
      Instant to,
      String zone,
      int onTime,
      int late,
      int omitted,
      List<AdherencePatternEmbeddable> patterns,
      int minimumOmissionDays,
      Instant consolidatedAt) {
    this.id = id;
    this.olderAdultId = owner;
    this.periodFrom = from;
    this.periodTo = to;
    this.calendarZone = zone;
    this.onTimeIntakes = onTime;
    this.lateIntakes = late;
    this.omittedIntakes = omitted;
    this.consolidatedAt = consolidatedAt;
    this.minimumOmissionDays = minimumOmissionDays;
    this.patterns = new ArrayList<>(List.copyOf(patterns));
  }

  /** Returns the deterministic snapshot identity. */
  public String id() {
    return id;
  }

  /** Returns the owner of the captured evidence. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the inclusive period start. */
  public Instant from() {
    return periodFrom;
  }

  /** Returns the exclusive period end. */
  public Instant to() {
    return periodTo;
  }

  /** Returns the captured calendar zone. */
  public String zone() {
    return calendarZone;
  }

  /** Returns the original consolidation instant. */
  public Instant consolidatedAt() {
    return consolidatedAt;
  }

  /** Returns the captured on-time count. */
  public int onTimeIntakes() {
    return onTimeIntakes;
  }

  /** Returns the captured late count. */
  public int lateIntakes() {
    return lateIntakes;
  }

  /** Returns the captured omission count. */
  public int omittedIntakes() {
    return omittedIntakes;
  }

  /** Returns the detection threshold used for this snapshot. */
  public int minimumOmissionDays() {
    return minimumOmissionDays;
  }

  /** Returns an immutable view of the ordered pattern evidence. */
  public List<AdherencePatternEmbeddable> patterns() {
    return List.copyOf(patterns);
  }
}
