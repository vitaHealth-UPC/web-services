package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads definitive intake history without inferring outcomes or regenerating the schedule. */
@Service
@Transactional(readOnly = true)
public class GetIntakeHistoryQueryHandler {
  private final IntakeRepository repository;

  /** Injects source evidence reads independently of persistence implementation. */
  public GetIntakeHistoryQueryHandler(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Returns ordered definitive evidence in a half-open interval of at most thirty days. */
  public List<IntakeResult> handle(String owner, Instant from, Instant to) {
    if (owner == null
        || owner.isBlank()
        || from == null
        || to == null
        || !to.isAfter(from)
        || Duration.between(from, to).compareTo(Duration.ofDays(30)) > 0)
      throw new IllegalArgumentException(
          "history requires an ordered range of at most thirty days");
    return repository.findAgenda(owner.trim(), from, to).stream()
        .filter(intake -> intake.status() != IntakeStatus.PENDING)
        .map(IntakeResultFromAggregateAssembler::toResult)
        .toList();
  }
}
