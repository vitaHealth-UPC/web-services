package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Requires an explicit voice-confirmation replacement choice. */
@Schema(description = "Request to turn voice confirmation of intakes on or off")
public record UpdateVoiceConfirmationResource(@Schema(example = "true") @NotNull Boolean enabled) {}
