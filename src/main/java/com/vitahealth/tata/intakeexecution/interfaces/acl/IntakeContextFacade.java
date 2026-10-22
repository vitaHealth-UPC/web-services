package com.vitahealth.tata.intakeexecution.interfaces.acl;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Publishes intake evidence reads and transactional verification to trusted context consumers. */
public interface IntakeContextFacade {
  /**
   * Reads the owner's source evidence in a validated half-open interval, including pending records.
   */
  List<IntakeResult> findEvidence(String olderAdultId, Instant from, Instant to);

  /** Returns a bounded ordered page of source owners after an exclusive identifier cursor. */
  List<String> findOlderAdultIdsWithIntakes(Instant from, Instant to, String after, int limit);

  /** Reads definitive outcomes over at most thirty elapsed days. */
  List<IntakeResult> history(String owner, Instant from, Instant to);

  /** Returns the earliest pending intake selected by the application clock. */
  Optional<IntakeResult> next(String owner);

  /** Resolves existing source evidence without granting user-facing authorization. */
  Optional<IntakeResult> findIntake(String id);

  /** Locks the source intake for the current transaction before deciding an omission. */
  boolean lockPendingIntake(String id);

  /** Locks and verifies the original recorded confirmation, including its timestamp. */
  boolean lockRecordedConfirmation(String id, Instant confirmedAt);
}
