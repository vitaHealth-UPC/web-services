package com.vitahealth.tata.intakeexecution.application.queryservices;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetNextIntakeQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.Optional;

/** Reads the next pending intake with explicit valid absence. */
public interface GetNextIntakeQueryService {
  /** Executes the typed selection and reports expected application rejections separately. */
  Result<Optional<IntakeResult>, ApplicationError> handle(GetNextIntakeQuery query);
}
