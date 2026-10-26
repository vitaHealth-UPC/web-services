package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.application.result.RecordedAlertEventResult;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import java.util.List;

/** Reads recorded events within the same authorized scope as alert detail. */
public interface GetAlertTimelineQueryService {
  List<RecordedAlertEventResult> handle(GetAlertDetailQuery query);
}
