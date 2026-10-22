package com.vitahealth.tata.intakeexecution.application.queryservices;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeDetailQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.Optional;

/** Reads stored intake evidence with explicit valid absence. */
public interface GetIntakeDetailQueryService {
  /** Executes the typed selection and reports expected application rejections separately. */
  Result<Optional<IntakeResult>, ApplicationError> handle(GetIntakeDetailQuery query);
}
