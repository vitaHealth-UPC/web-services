package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Requires the user's explicit reduced-motion replacement choice. */
@Schema(description = "Request to turn reduced motion on or off")
public record UpdateReducedMotionResource(@Schema(example = "true") @NotNull Boolean enabled) {}
