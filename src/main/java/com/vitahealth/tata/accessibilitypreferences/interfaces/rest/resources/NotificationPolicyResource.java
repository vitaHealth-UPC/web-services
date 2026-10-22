package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/** Delays are measured from scheduled intake time; null disables the daily summary. */
@Schema(
    description =
        "Delays in minutes from scheduled intake time, strictly increasing from reminder to"
            + " escalation")
public record NotificationPolicyResource(
    @Schema(
            example = "10",
            minimum = "1",
            maximum = "1440",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int reminderMinutes,
    @Schema(
            example = "20",
            minimum = "1",
            maximum = "1440",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int pendingAlertMinutes,
    @Schema(
            example = "120",
            minimum = "1",
            maximum = "1440",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int omittedAlertMinutes,
    @Schema(
            example = "180",
            minimum = "1",
            maximum = "1440",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int escalationMinutes,
    @Schema(
            description = "Local time with minute precision; null disables the summary",
            example = "20:00",
            nullable = true)
        LocalTime dailySummaryTime) {}
