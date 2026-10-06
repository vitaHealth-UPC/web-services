package com.tata.omissionescalation.domain.repositories;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IOmissionCaseRepository {

  OmissionCase save(OmissionCase omissionCase);

  Optional<OmissionCase> findById(Long id);

  Optional<OmissionCase> findByIntakeId(Long intakeId);

  /** Pending cases whose grace period ended at or before {@code now}. */
  List<OmissionCase> findExpiredPending(Instant now);

  /** Cases already omitted that may still need to be escalated. */
  List<OmissionCase> findOmittedOrEscalated();
}
