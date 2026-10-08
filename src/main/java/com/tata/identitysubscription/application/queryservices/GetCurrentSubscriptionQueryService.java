package com.tata.identitysubscription.application.queryservices;

import com.tata.identitysubscription.application.models.SubscriptionResult;

public interface GetCurrentSubscriptionQueryService {
    SubscriptionResult handle(String accountId);
}
