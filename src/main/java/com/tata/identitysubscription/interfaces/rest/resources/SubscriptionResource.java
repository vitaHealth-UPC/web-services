package com.tata.identitysubscription.interfaces.rest.resources;

import com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResource(
        String accountId,
        PlanResource plan,
        SubscriptionStatus status,
        Instant renewsAt
) {
}
