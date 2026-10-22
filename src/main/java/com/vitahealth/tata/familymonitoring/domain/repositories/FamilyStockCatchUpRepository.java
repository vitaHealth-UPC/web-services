package com.vitahealth.tata.familymonitoring.domain.repositories;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Stores resumable initial-stock work independently from follow-up history and transport. */
public interface FamilyStockCatchUpRepository {
  /** Creates work in the monitor creation transaction; an existing request keeps its progress. */
  void enqueue(FamilyStockCatchUp request);

  /** Reads detached recovery evidence without reserving it. */
  Optional<FamilyStockCatchUp> find(long monitorId);

  /** Reserves and refreshes one request until the caller transaction ends. */
  Optional<FamilyStockCatchUp> reserve(long monitorId);

  /** Selects at most 100 eligible requests after an exclusive monitor identity. */
  List<FamilyStockCatchUp> findDueAfter(Instant cutoff, long afterMonitorId, int limit);

  /** Replaces exactly observed work; stale retry writers cannot reset progress or recreate it. */
  boolean replace(FamilyStockCatchUp expected, FamilyStockCatchUp replacement);

  /** Completes exactly observed work in the same transaction as the last projection step. */
  boolean complete(FamilyStockCatchUp expected);
}
