package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetIntakeDetail read inside a read-only transaction. */
@Service
@Transactional(readOnly = true)
public class GetIntakeDetailQueryHandler {
  private final IntakeRepository repository;

  /** Injects the evidence read dependencies. */
  public GetIntakeDetailQueryHandler(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Returns stored evidence, with an empty Optional for valid absence. */
  public Optional<IntakeResult> handle(String intakeId) {
    if (intakeId == null || intakeId.isBlank()) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VALIDATION_ERROR, "intakeId is required");
    }
    return repository.findById(intakeId.trim()).map(IntakeResultFromAggregateAssembler::toResult);
  }
}
