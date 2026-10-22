package com.vitahealth.tata.adherenceanalytics.application.queryservices;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceSnapshotQuery;
import java.util.Optional;

/** Reads immutable historical consolidation evidence without running a new calculation. */
public interface AdherenceSnapshotQueryService {
  /** Returns absence for unknown snapshots or a different adult owner. */
  Optional<AdherencePeriodSnapshot> handle(GetAdherenceSnapshotQuery query);
}
