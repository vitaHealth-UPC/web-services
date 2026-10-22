package com.vitahealth.tata.intakeexecution.application.queryservices;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeAgendaQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;

/** Reads agenda evidence; an empty selection is a successful result. */
public interface GetIntakeAgendaQueryService {
  /** Executes the typed selection and reports expected application rejections separately. */
  Result<List<IntakeResult>, ApplicationError> handle(GetIntakeAgendaQuery query);
}
