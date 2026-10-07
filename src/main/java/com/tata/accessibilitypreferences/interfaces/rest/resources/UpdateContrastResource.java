package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to turn high contrast on or off")
public record UpdateContrastResource(@Schema(example = "true") boolean enabled) {}
