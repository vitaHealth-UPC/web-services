package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/** Delays are measured from scheduled intake time; null disables the daily summary. */
public record NotificationPolicyResource(
    @Schema(example = "10") int reminderMinutes,
    @Schema(example = "20") int pendingAlertMinutes,
    @Schema(example = "120") int omittedAlertMinutes,
    @Schema(example = "180") int escalationMinutes,
    @Schema(example = "20:00", nullable = true) LocalTime dailySummaryTime) {}
