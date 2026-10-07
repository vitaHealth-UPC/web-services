package com.tata.identitysubscription.domain.model.events;

import java.time.Instant;

/** Published by Identity & Subscription when an account becomes enabled. */
public record AccountEnabled(String accountId, Instant occurredAt) {}
