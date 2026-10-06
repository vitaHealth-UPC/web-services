package com.tata.identitysubscription.application.internal.queryservices;

import com.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.tata.identitysubscription.application.models.PlanResult;
import com.tata.identitysubscription.domain.services.PlanCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListAvailablePlansQueryHandler {
    public List<PlanResult> handle() {
        return PlanCatalog.availablePlans().stream()
                .map(SubscriptionMapper::toResult)
                .toList();
    }
}
