package com.vitahealth.tata.adherenceanalytics.application.result;

import java.time.Instant;

/** One recent definitive intake displayed in adherence history. */
public record RecentAdherenceIntakeResult(
    Instant scheduledAt, String medicationName, String status, Integer minutesLate) {}
