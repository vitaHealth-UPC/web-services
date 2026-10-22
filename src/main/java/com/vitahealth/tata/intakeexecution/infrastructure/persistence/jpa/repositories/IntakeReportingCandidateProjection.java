package com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories;

import java.time.Instant;

/**
 * Reads only the reference and ordering key, without reconstructing potentially invalid evidence.
 */
public interface IntakeReportingCandidateProjection {
  /** Stored evidence identifier used for the final transactional lookup. */
  String getId();

  /** Scheduled instant used with the identifier as a unique scan position. */
  Instant getScheduledAt();
}
