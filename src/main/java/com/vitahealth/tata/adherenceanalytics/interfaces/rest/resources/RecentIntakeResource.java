package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Definitive intake evidence displayed in recent history. */
@Schema(
    description =
        "status is CONFIRMED (on time), LATE or OMITTED; minutesLate only applies to LATE")
public record RecentIntakeResource(
    Instant scheduledAt, String medicationName, String status, Integer minutesLate) {}
