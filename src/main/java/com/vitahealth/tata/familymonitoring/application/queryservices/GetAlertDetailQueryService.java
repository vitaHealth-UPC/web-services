package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;

/** Reads alert evidence only within the requesting caregiver relationship. */
public interface GetAlertDetailQueryService {
  /** Returns a stable snapshot of the selected alert and its original occurrence. */
  AlertResult handle(GetAlertDetailQuery query);
}
