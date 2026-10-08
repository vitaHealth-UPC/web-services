package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.application.queryservices.OlderAdultStatusView;
import com.tata.familymonitoring.domain.model.queries.GetOlderAdultStatusQuery;

public interface GetOlderAdultStatusQueryService {
    OlderAdultStatusView handle(GetOlderAdultStatusQuery query);
}
