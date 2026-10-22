package com.vitahealth.tata.identitysubscription.application.result;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import java.time.Instant;

/** Account subscription projection; a missing renewal instant represents no scheduled renewal. */
public record SubscriptionResult(
    String accountId, PlanResult plan, SubscriptionStatus status, Instant renewsAt) {}
