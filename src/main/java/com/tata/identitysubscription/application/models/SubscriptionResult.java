package com.tata.identitysubscription.application.models;

import com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResult(
        String accountId,
        PlanResult plan,
        SubscriptionStatus status,
        Instant renewsAt
) {
}
