package com.vitahealth.tata.adherenceanalytics.application.internal.commandservices;

import com.vitahealth.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.exceptions.AdherenceSnapshotAlreadyExistsException;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Captures immutable period evidence and publishes its omission patterns. */
@Service
public class ConsolidateWeeklyPeriodCommandHandler implements AdherenceConsolidationCommandService {
  private final AdherenceQueryService queries;
  private final AdherenceSnapshotRepository snapshots;
  private final PersistAdherenceSnapshotCommandHandler persistence;
  private final Clock clock;

  /**
   * Injects source analysis, owned storage, atomic persistence and the common application clock.
   */
  public ConsolidateWeeklyPeriodCommandHandler(
      AdherenceQueryService queries,
      AdherenceSnapshotRepository snapshots,
      PersistAdherenceSnapshotCommandHandler persistence,
      Clock clock) {
    this.queries = queries;
    this.snapshots = snapshots;
    this.persistence = persistence;
    this.clock = clock;
  }

  /** Preserves stored period identity and original consolidation evidence on replay. */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public AdherencePeriodSnapshot handle(ConsolidateWeeklyPeriodCommand command) {
    var period = command.period();
    var from = period.from();
    var to = period.to();
    var existing = snapshots.find(period.snapshotId());
    if (existing.isPresent()) return existing.orElseThrow();
    var analysis =
        queries.capture(
            new GetAdherencePeriodQuery(period.olderAdultId(), from, to, period.zone()));
    var metrics = analysis.metrics();
    var candidate =
        new AdherencePeriodSnapshot(
            period.snapshotId(),
            metrics.olderAdultId(),
            from,
            to,
            period.zone(),
            metrics.onTimeIntakes(),
            metrics.lateIntakes(),
            metrics.omittedIntakes(),
            analysis.patterns(),
            analysis.minimumOmissionDays(),
            clock.instant());
    try {
      return persistence.handle(candidate);
    } catch (AdherenceSnapshotAlreadyExistsException collision) {
      return snapshots.find(candidate.id()).orElseThrow(() -> collision);
    }
  }
}
