package com.vitahealth.tata.omissionescalation.domain.repositories;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Stores omission evidence and selects active work without exposing persistence entities. */
public interface IOmissionCaseRepository {

  /**
   * Persists the aggregate and owned history, returning their stored identities and audit times.
   */
  OmissionCase save(OmissionCase omissionCase);

  /** Returns the current aggregate snapshot without reserving it for a subsequent decision. */
  Optional<OmissionCase> findById(Long id);

  /** Locks and refreshes the case within the caller's required decision transaction. */
  Optional<OmissionCase> findByIdForUpdate(Long id);

  /** Looks up the single omission case associated with the original scheduled intake. */
  Optional<OmissionCase> findByIntakeId(String intakeId);

  /** Highest recorded level for existing cases in the selected adult and intake scope. */
  Map<String, EscalationLevel> findAttentionLevels(String olderAdultId, Set<String> intakeIds);

  /** Pending cases whose grace period ended at or before {@code now}. */
  List<Long> findExpiredPendingIdsAfter(Instant now, long afterId, int limit);

  /** Returns ordered active case IDs after the cursor whose business alert has not been created. */
  List<Long> findActiveCaseIdsWithoutAlertAfter(long afterId, int limit);

  /** Cases already omitted that may still need to be escalated. */
  List<Long> findActiveCaseIdsAfter(long afterId, int limit);
}
