package com.vitahealth.tata.adherenceanalytics.application.internal.commandservices;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.AdherenceEventPublisher;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.vitahealth.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commits immutable period evidence and all synchronous projections as one independent unit. */
@Service
public class PersistAdherenceSnapshotCommandHandler {
  private final AdherenceSnapshotRepository snapshots;
  private final AdherenceEventPublisher events;

  /** Injects owned storage and typed synchronous publication. */
  public PersistAdherenceSnapshotCommandHandler(
      AdherenceSnapshotRepository snapshots, AdherenceEventPublisher events) {
    this.snapshots = snapshots;
    this.events = events;
  }

  /**
   * Existing evidence is returned silently; only the successful inserting transaction publishes.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AdherencePeriodSnapshot handle(AdherencePeriodSnapshot candidate) {
    var existing = snapshots.find(candidate.id());
    if (existing.isPresent()) return existing.orElseThrow();
    snapshots.insert(candidate);
    for (var pattern : candidate.patterns()) {
      events.publish(
          new AdherencePatternDetected(
              candidate.olderAdultId(),
              pattern.medicationId(),
              pattern.omissionDays(),
              pattern.firstDay(),
              pattern.lastDay(),
              candidate.consolidatedAt()));
    }
    return candidate;
  }
}
