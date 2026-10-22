package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetNextIntake read inside a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class GetNextIntakeQueryHandler {
  private final IntakeRepository repository;
  private final Clock clock;

  /** Injects the evidence read dependencies. */
  public GetNextIntakeQueryHandler(IntakeRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = Objects.requireNonNull(clock);
  }

  /** Returns stored evidence, with an empty Optional for valid absence. */
  public Optional<IntakeResult> handle(String olderAdultId) {
    if (olderAdultId == null || olderAdultId.isBlank()) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VALIDATION_ERROR, "olderAdultId is required");
    }
    return repository
        .findNextPendingByOlderAdultId(olderAdultId.trim(), clock.instant())
        .map(IntakeResultFromAggregateAssembler::toResult);
  }
}
