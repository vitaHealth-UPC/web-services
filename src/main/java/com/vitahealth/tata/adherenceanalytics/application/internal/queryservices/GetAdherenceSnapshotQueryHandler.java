package com.vitahealth.tata.adherenceanalytics.application.internal.queryservices;

import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceSnapshotQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceSnapshotQuery;
import com.vitahealth.tata.adherenceanalytics.domain.repositories.AdherenceSnapshotRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads owned historical evidence and its patterns inside a single read transaction. */
@Service
@Transactional(readOnly = true)
public class GetAdherenceSnapshotQueryHandler implements AdherenceSnapshotQueryService {
  private final AdherenceSnapshotRepository snapshots;

  /** Injects only analytics-owned evidence storage. */
  public GetAdherenceSnapshotQueryHandler(AdherenceSnapshotRepository snapshots) {
    this.snapshots = snapshots;
  }

  /** Keeps foreign owner snapshots indistinguishable from missing evidence. */
  @Override
  public Optional<AdherencePeriodSnapshot> handle(GetAdherenceSnapshotQuery query) {
    return snapshots
        .find(query.snapshotId())
        .filter(snapshot -> snapshot.olderAdultId().equals(query.olderAdultId()));
  }
}
