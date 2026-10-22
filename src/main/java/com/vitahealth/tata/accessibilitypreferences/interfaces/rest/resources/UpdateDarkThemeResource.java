package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Requires an explicit replacement choice rather than interpreting omission as disabling dark
 * theme.
 */
@Schema(description = "Request to turn dark theme on or off")
public record UpdateDarkThemeResource(@Schema(example = "true") @NotNull Boolean enabled) {}
