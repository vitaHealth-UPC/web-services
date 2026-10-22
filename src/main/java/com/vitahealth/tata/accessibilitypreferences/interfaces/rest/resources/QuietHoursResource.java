package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** Carries both local-time boundaries; domain conversion requires minute precision. */
@Schema(description = "Daily interval without non-critical notifications. It may cross midnight")
public record QuietHoursResource(
    @Schema(type = "string", format = "time", example = "22:00") @NotNull LocalTime start,
    @Schema(type = "string", format = "time", example = "07:00") @NotNull LocalTime end) {}
