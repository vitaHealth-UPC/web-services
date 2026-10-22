package com.vitahealth.tata.familymonitoring.domain.repositories;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import java.util.List;
import java.util.Optional;

/** Persists relationship-scoped follow-up without assuming unique adult or caregiver history. */
public interface IFamilyMonitorRepository {

  /** Selects ordered follow-up identities for both participants, including revoked history. */
  List<Long> findIdsByParticipantsAfter(
      String olderAdultId, String caregiverId, long afterId, int limit);

  /** Restores a snapshot by its stored identity without reserving it for a write. */
  Optional<FamilyMonitor> findById(Long id);

  /** Selects a bounded ordered page of relationship follow-up identities for source projection. */
  List<Long> findIdsByOlderAdultIdAfter(String olderAdultId, long afterId, int limit);

  /** Reserves and refreshes one follow-up within the caller's required write transaction. */
  Optional<FamilyMonitor> findByIdForUpdate(Long id);

  /** Stores a snapshot while preserving child identities and rejecting obsolete revisions. */
  FamilyMonitor save(FamilyMonitor monitor);

  /** Restores the exact relationship history, regardless of its current consent state. */
  Optional<FamilyMonitor> findByCareLinkId(String careLinkId);
}
