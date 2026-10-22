package com.vitahealth.tata.intakeexecution.domain.repositories;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeReportingCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence contract for intake evidence, agendas and transactional state transitions. */
public interface IntakeRepository {
  /** Returns evidence in the half-open interval, ordered by schedule and identifier. */
  List<Intake> findAgenda(String olderAdultId, Instant from, Instant to);

  /** Persists new or revised aggregates while retaining their identifiers. */
  List<Intake> saveAll(List<Intake> intakes);

  /** Looks up evidence without granting a state-transition lock. */
  Optional<Intake> findById(String id);

  /** Locks and refreshes evidence within the caller transaction before a state transition. */
  Optional<Intake> findByIdForConfirmation(String id);

  /**
   * Reads at most limit references after an optional exclusive schedule/id cursor, without locks.
   */
  List<IntakeReportingCandidate> findUnreportedPendingDueAfter(
      Instant cutoff, IntakeReportingCandidate after, int limit);

  /** Reads at most limit distinct owners after the exclusive cursor, ordered by owner identity. */
  List<String> findOlderAdultIdsWithIntakes(Instant from, Instant to, String after, int limit);

  /** Returns treatment evidence at or after the inclusive lower bound. */
  List<Intake> findFutureByTreatmentId(String treatmentId, Instant from);

  /** Locks current future slots in stable order while their schedule is reconciled. */
  List<Intake> findFutureByTreatmentIdForUpdate(String treatmentId, Instant from);

  /** Removes explicitly selected pending evidence during reconciliation. */
  void deleteAll(List<Intake> intakes);

  /** Returns the earliest pending slot at or after the bound, breaking ties by identifier. */
  Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from);
}
