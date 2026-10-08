package com.tata.identitysubscription.interfaces.rest.transform;

import com.tata.identitysubscription.application.models.PlanResult;
import com.tata.identitysubscription.application.models.SubscriptionResult;
import com.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;
import com.tata.identitysubscription.interfaces.rest.resources.ChangeSubscriptionResource;
import com.tata.identitysubscription.interfaces.rest.resources.PlanResource;
import com.tata.identitysubscription.interfaces.rest.resources.SubscriptionResource;

public final class SubscriptionResourceAssembler {
    private SubscriptionResourceAssembler() {
    }

    public static PlanResource toResource(PlanResult result) {
        return new PlanResource(
                result.code(),
                result.name(),
                result.monthlyPrice(),
                result.currency(),
                result.capabilities()
        );
    }

    public static SubscriptionResource toResource(SubscriptionResult result) {
        return new SubscriptionResource(
                result.accountId(),
                toResource(result.plan()),
                result.status(),
                result.renewsAt()
        );
    }

    public static ChangeSubscriptionCommand toCommand(
            String accountId,
            ChangeSubscriptionResource resource
    ) {
        return new ChangeSubscriptionCommand(accountId, resource.planCode());
    }
}
