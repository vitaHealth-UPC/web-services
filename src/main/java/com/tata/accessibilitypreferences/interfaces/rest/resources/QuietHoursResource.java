package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

@Schema(description = "Daily interval without non-critical notifications. It may cross midnight")
public record QuietHoursResource(
        @Schema(type = "string", format = "time", example = "22:00") @NotNull LocalTime start,
        @Schema(type = "string", format = "time", example = "07:00") @NotNull LocalTime end
) {}
