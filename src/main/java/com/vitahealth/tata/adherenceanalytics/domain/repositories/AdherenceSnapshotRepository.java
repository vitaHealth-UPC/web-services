package com.vitahealth.tata.adherenceanalytics.domain.repositories;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import java.util.Optional;

/** Stores immutable period evidence without owning application transaction orchestration. */
public interface AdherenceSnapshotRepository {
  /** Restores original evidence without recalculating its metrics or patterns. */
  Optional<AdherencePeriodSnapshot> find(String id);

  /** Inserts within the caller transaction; a conflicting identity is an explicit collision. */
  void insert(AdherencePeriodSnapshot snapshot);
}
