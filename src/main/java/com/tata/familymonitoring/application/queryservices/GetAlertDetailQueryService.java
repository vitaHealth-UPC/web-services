package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;

public interface GetAlertDetailQueryService {
    AlertSummary handle(GetAlertDetailQuery query);
}
