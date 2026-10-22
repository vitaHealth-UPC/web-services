package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import java.time.Instant;

/** Exposes current account subscription state and its recorded renewal instant. */
public record SubscriptionResource(
    String accountId, PlanResource plan, SubscriptionStatus status, Instant renewsAt) {}
