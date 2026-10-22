package com.vitahealth.tata.omissionescalation.application.result;

import java.time.Instant;

/** Durable intent identity and original request time; this receipt is not a delivery result. */
public record NotificationIntentReceipt(String intentId, Instant requestedAt) {}
