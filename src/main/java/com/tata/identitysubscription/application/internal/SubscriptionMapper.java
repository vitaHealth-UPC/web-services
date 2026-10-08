package com.tata.identitysubscription.application.internal;

import com.tata.identitysubscription.application.models.PlanResult;
import com.tata.identitysubscription.application.models.SubscriptionResult;
import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.entities.Plan;

public final class SubscriptionMapper {
    private SubscriptionMapper() {
    }

    public static PlanResult toResult(Plan plan) {
        return new PlanResult(
                plan.code(),
                plan.name(),
                plan.monthlyPrice(),
                plan.currency(),
                plan.capabilities()
        );
    }

    public static SubscriptionResult toResult(Account account, Plan plan) {
        return new SubscriptionResult(
                account.id(),
                toResult(plan),
                account.subscriptionStatus(),
                account.subscriptionRenewsAt()
        );
    }
}
