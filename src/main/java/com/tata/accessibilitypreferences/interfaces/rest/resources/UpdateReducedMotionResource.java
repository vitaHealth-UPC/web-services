package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to turn reduced motion on or off")
public record UpdateReducedMotionResource(@Schema(example = "true") boolean enabled) {}
