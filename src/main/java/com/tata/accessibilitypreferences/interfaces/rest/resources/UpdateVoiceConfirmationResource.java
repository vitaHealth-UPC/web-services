package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to turn voice confirmation of intakes on or off")
public record UpdateVoiceConfirmationResource(@Schema(example = "true") boolean enabled) {}
