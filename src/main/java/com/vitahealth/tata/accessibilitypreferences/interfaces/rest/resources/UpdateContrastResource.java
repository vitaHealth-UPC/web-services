package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Requires an explicit replacement choice rather than interpreting omission as disabling contrast.
 */
@Schema(description = "Request to turn high contrast on or off")
public record UpdateContrastResource(@Schema(example = "true") @NotNull Boolean enabled) {}
