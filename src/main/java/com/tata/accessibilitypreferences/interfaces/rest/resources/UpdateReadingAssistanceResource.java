package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to turn reading assistance on or off")
public record UpdateReadingAssistanceResource(@Schema(example = "true") boolean enabled) {}
