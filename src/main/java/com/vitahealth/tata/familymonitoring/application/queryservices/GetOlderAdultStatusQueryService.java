package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.application.result.OlderAdultStatusResult;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;

/** Reads recent adult evidence and follow-up awaiting explicit caregiver closure. */
public interface GetOlderAdultStatusQueryService {
  /** Returns immutable alert, stock and analytics snapshots for the selected relationship. */
  OlderAdultStatusResult handle(GetOlderAdultStatusQuery query);
}
