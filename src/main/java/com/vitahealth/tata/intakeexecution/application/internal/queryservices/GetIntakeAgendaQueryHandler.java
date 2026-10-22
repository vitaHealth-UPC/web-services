package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetIntakeAgenda read inside a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class GetIntakeAgendaQueryHandler {
  private final IntakeRepository repository;

  /** Injects the evidence read dependencies. */
  public GetIntakeAgendaQueryHandler(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Returns ordered evidence in a validated range of at most eight elapsed days. */
  public List<IntakeResult> handle(String olderAdultId, Instant from, Instant to) {
    if (olderAdultId == null
        || olderAdultId.isBlank()
        || from == null
        || to == null
        || !to.isAfter(from)
        || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VALIDATION_ERROR,
          "agenda requires an older adult and an ordered range of at most eight days");
    }
    return repository.findAgenda(olderAdultId.trim(), from, to).stream()
        .map(IntakeResultFromAggregateAssembler::toResult)
        .toList();
  }
}
