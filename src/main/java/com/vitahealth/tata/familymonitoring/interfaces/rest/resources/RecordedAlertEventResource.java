package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import java.time.Instant;

/** Public recorded-event response consumed by Android alert detail. */
public record RecordedAlertEventResource(String type, Instant occurredAt) {}
