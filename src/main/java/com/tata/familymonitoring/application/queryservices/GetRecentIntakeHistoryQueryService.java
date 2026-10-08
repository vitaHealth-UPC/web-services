package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.queries.GetRecentIntakeHistoryQuery;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import java.util.List;

public interface GetRecentIntakeHistoryQueryService {
    List<IntakeSummary> handle(GetRecentIntakeHistoryQuery query);
}
