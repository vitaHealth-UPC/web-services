package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Requires an explicit reading-assistance replacement choice. */
@Schema(description = "Request to turn reading assistance on or off")
public record UpdateReadingAssistanceResource(@Schema(example = "true") @NotNull Boolean enabled) {}
