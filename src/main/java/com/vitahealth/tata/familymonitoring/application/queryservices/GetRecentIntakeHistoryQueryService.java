package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.domain.model.queries.GetRecentIntakeHistoryQuery;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import java.util.List;

/** Reads recent source intake summaries for an authorized monitoring relationship. */
public interface GetRecentIntakeHistoryQueryService {
  List<IntakeSummary> handle(GetRecentIntakeHistoryQuery query);
}
