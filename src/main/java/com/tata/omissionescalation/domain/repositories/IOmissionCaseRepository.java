package com.tata.omissionescalation.domain.repositories;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IOmissionCaseRepository {

  OmissionCase save(OmissionCase omissionCase);

  Optional<OmissionCase> findById(Long id);
  default Optional<OmissionCase> findByIdForUpdate(Long id) { return findById(id); }

  Optional<OmissionCase> findByIntakeId(String intakeId);
  default Optional<OmissionCase> findByIntakeIdForUpdate(String id) { return findByIntakeId(id); }

  /** Pending cases whose grace period ended at or before {@code now}. */
  List<OmissionCase> findExpiredPending(Instant now);

  /** Cases already omitted that may still need to be escalated. */
  List<OmissionCase> findOmittedOrEscalated();
}
