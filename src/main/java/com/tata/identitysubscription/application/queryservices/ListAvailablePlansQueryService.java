package com.tata.identitysubscription.application.queryservices;

import com.tata.identitysubscription.application.models.PlanResult;
import java.util.List;

public interface ListAvailablePlansQueryService {
    List<PlanResult> handle();
}
